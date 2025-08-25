#include <Arduino.h>
#include <esp_camera.h>    // Provides camera_config_t and esp_camera_init()
#include "camera_module.h"
#include "board_config.h"

/*-----------------------------------------------------------------------------
  initCamera()
  Initializes the camera with pins from board_config.h -> camera_pins.h.
  - Chooses frame buffer location and count based on PSRAM availability.
  - Sets default image parameters and sensor flips depending on PID/model.
  - On failure, logs the error and restarts the MCU to recover.
-----------------------------------------------------------------------------*/
void initCamera() {
  Serial.printf("📦 PSRAM found: %s\n", psramFound() ? "Yes" : "No");

  // Base configuration (pins are provided by board_config.h / camera_pins.h).
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;

  // 20 MHz XCLK is a stable 
  config.xclk_freq_hz = 20000000;

  // Start with high resolution; adjust below based on JPEG/PSRAM.
  config.frame_size = FRAMESIZE_UXGA;
  config.pixel_format = PIXFORMAT_JPEG;

  // Frame grab behavior and buffering strategy.
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 12;     // Lower = better quality (but larger file)
  config.fb_count = 1;          // Frame buffers (in PSRAM if available)

  // Optimize for JPEG + PSRAM: improve throughput and latency with 2 FBs.
  if (config.pixel_format == PIXFORMAT_JPEG && psramFound()) {
    config.jpeg_quality = 10;
    config.fb_count = 2;
    config.grab_mode = CAMERA_GRAB_LATEST;
  } else {
    // Fallback for limited RAM scenarios.
    config.frame_size = FRAMESIZE_SVGA;
    config.fb_location = CAMERA_FB_IN_DRAM;
  }

  // Initialize camera; reboot on failure to recover cleanly.
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x\n", err);
    ESP.restart();
  }

  // Access sensor to tweak orientation and image parameters.
  sensor_t *s = esp_camera_sensor_get();
  if (!s) {
    Serial.println("Failed to get camera sensor");
    ESP.restart();
  }

  // Known-good tweaks for OV3660-based modules. Improves readibility of 
  // image text
  if (s->id.PID == OV3660_PID) {
    s->set_vflip(s, 1);
    s->set_brightness(s, 1);
    s->set_saturation(s, -2);
  }

  // Reduce framesize for latency/size if still using JPEG. (QVGA is 320x240)
  if (config.pixel_format == PIXFORMAT_JPEG) {
    s->set_framesize(s, FRAMESIZE_QVGA);
  }

  // Horizontal mirror common for “selfie” orientation mounting.
  // Makes is so text is the corecct way
  s->set_hmirror(s, 1);
  // flips vertically so text isn't upside down
  s->set_vflip(s, 1);                           

  // Board-specific orientation overrides.
#if defined(CAMERA_MODEL_M5STACK_WIDE) || defined(CAMERA_MODEL_M5STACK_ESP32CAM)
  s->set_vflip(s, 1);
  s->set_hmirror(s, 1);
#endif

#if defined(CAMERA_MODEL_ESP32S3_EYE)
  s->set_vflip(s, 1);
#endif
}
