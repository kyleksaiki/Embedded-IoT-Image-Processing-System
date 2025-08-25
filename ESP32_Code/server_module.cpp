#include "server_module.h"
#include <WiFi.h>
#include <HTTPClient.h>
#include "esp_camera.h"

// Endpoint of your Spring Boot server upload controller.
// Example expects Content-Type: image/jpeg and returns a single character.
const char* url = "http://192.168.0.179:8080/upload";

// Helper: Map single-character server response to an ActionResult.
// Validates server response and checks that image has been read
// Server will return 'E' if image cannot be read
static ActionResult mapCharToAction(char c) {
    switch (c) {
        case 'A': return ACTION_A;
        case 'B': return ACTION_B;
        case 'C': return ACTION_C;
        case 'D': return ACTION_D;
        default:
            Serial.printf("Invalid action char from server: '%c'\n", c);
            return ACTION_ERROR;
    }
}

ActionResult captureAndSend() {
  // Acquire a frame buffer from the camera driver.
  camera_fb_t* fb = esp_camera_fb_get();
  if (!fb) return ACTION_ERROR;

  // New client/socket per request to avoid stuck half-open connections.
  WiFiClient client;
  HTTPClient http;
  http.setReuse(false);           // Do not keep-alive
  http.setConnectTimeout(15000);  // TCP connect timeout (ms)
  http.setTimeout(60000);         // Read/overall timeout (ms), generous for server work
  // http.useHTTP10(true);        // Optional: force HTTP/1.0 to disable chunked encoding

  // Initialize HTTP session with provided client and URL.
  if (!http.begin(client, url)) {
    esp_camera_fb_return(fb);     // Always release the frame buffer on exit paths
    return ACTION_ERROR;
  }

  // Set headers for raw JPEG upload; request server to close connection after.
  http.addHeader("Content-Type", "image/jpeg");
  http.addHeader("Connection", "close");

  // Synchronous POST of the image bytes; returns HTTP status code.
  int code = http.POST(fb->buf, fb->len);

  // Release camera frame as soon as the POST is complete.
  esp_camera_fb_return(fb);

  // Read body only if request reached the HTTP layer (code > 0).
  String body;
  if (code > 0) body = http.getString();
  Serial.printf("HTTP %d (%s) body='%s'\n",
                code, http.errorToString(code).c_str(), body.c_str());

  // Cleanup HTTP resources and hard-close TCP to avoid TIME_WAIT pileup.
  http.end();
  client.stop();

  // Non-OK → error.
  if (code != 200) return ACTION_ERROR;

  // Expect exactly one character (e.g., "A").
  body.trim();
  if (body.length() != 1) return ACTION_ERROR;

  // Map to enum understood by the vibration layer.
  return mapCharToAction(body[0]);
}
