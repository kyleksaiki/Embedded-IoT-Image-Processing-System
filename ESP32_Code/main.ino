/*------------------------------------------------------------------------------
  Project: ESP32 (ESP32-S3 EYE) camera -> Spring Boot -> haptic feedback
  File:    main (ties together Wi-Fi, camera, server upload, button, vibration)

  Flow:
    - setup(): start Serial, init button & vibration, connect Wi-Fi, init camera,
               (optionally start web stream to check camera is working), give a 3-pulse “ready” vibration.
    - loop(): on button press, capture a picture, POST to Spring Boot endpoint,
              map 1-char response (A/B/C/D) to 1..4 vibration pulses.

  Modules used:
    - wifi_module.*        : Wi-Fi bring-up and IP logging
    - camera_module.*      : Camera pin config + sensor tuning
    - server_module.*      : capture frame, HTTP POST, parse 1-char response
    - button_module.*      : active-LOW button with simple debounce
    - vibration_module.*   : GPIO pulse output for haptic motor

  Notes:
    - Camera model selection is in board_config.h.
    - Start-camera-server code is present but commented out for now. Uncomment to check what the camera's view
    - Make sure Spring Boot application is running before running this code
  
------------------------------------------------------------------------------*/

#include "esp_camera.h"
#include "wifi_module.h"
#include "camera_module.h"
#include "server_module.h"
#include "button_module.h"
#include "vibration_module.h"
#include <WiFi.h>

// ===========================
// Select camera model in board_config.h
// ===========================
#include "board_config.h"

// ===========================
// Enter your WiFi credentials
// (Handled inside wifi_module.cpp for this build.)
// ===========================

void startCameraServer();  // optional: RTSP/HTTP stream server 
void setupLedFlash();      // optional: LED flash helper 

void setup() {
  Serial.begin(115200);
  Serial.setDebugOutput(true);  // SDK logs on Serial
  delay(1000);                  // small settle delay for USB/Serial

  // --- Local peripherals ---
  initButton();                 // configure button GPIO w/ pull-up
  initVibration();              // configure motor/driver GPIO (off by default)

  // --- Network ---
  initWiFi();                   // connect to AP; prints IP or restarts on fail

  // --- Imaging ---
  initCamera();                 // set up camera pins, frame buffers, and sensor

  /*
  // Optional: start a live camera web server
  // Uncomment to view what the camera is seeing (important for checking your hardware is correct)
  startCameraServer();
  Serial.println("📷 Camera ready!");
  Serial.print("🌐 Stream URL: http://");
  Serial.print(WiFi.localIP());
  Serial.println("/");
  */
  vibratePulses(3);             // “ready” signal: 3 short pulses, ensure vibrator is working
}

void loop() {
  // Edge-triggered check for button press (active LOW).
  if (buttonPressed()) {
   
    // Capture a frame, POST to server, and get an ActionResult back.
    ActionResult r = captureAndSend();

    // Map ActionResult to haptic feedback (pulse count).
    switch (r) {
    case ACTION_A: 
      Serial.println("Got A"); 
      vibratePulses(1);
      break;
    case ACTION_B: 
      Serial.println("Got B"); 
      vibratePulses(2);
      break;
    case ACTION_C: 
      Serial.println("Got C"); 
      vibratePulses(3);
      break;
    case ACTION_D: 
      Serial.println("Got D"); 
      vibratePulses(4);
      break;
    case ACTION_ERROR:
    default:
      Serial.println("Error or invalid response");
      break;
    }
  } 
}
