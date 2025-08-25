#include <Arduino.h>
#include <WiFi.h>
//#include <HTTPClient.h>    // Included for parity with other modules; not used here.
//#include <ArduinoJson.h>   // Included for parity with other modules; not used here.

#include "wifi_module.h"

// Plaintext for demo only; prefer storing credentials in NVS or provisioning.
const char* ssid = "MSNS";
const char* password = "3bicycle";

void initWiFi() {
  // Log target SSID (sent as UTF-8 bytes to avoid source encoding issues).
  Serial.printf("\xF0\x9F\x93\xB1 Connecting to SSID: %s\n", ssid);

  // Begin connection and disable Wi-Fi power-save to maximize throughput/stability.
  WiFi.begin(ssid, password);
  WiFi.setSleep(false);

  // Basic connection timeout loop (15s total, prints a dot every 500ms).
  unsigned long startAttemptTime = millis();
  const unsigned long timeout = 15000;

  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < timeout) {
    delay(500);
    Serial.print(".");
  }

  // If not connected by the deadline, reboot to recover state.
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\nWiFi connection failed. Restarting...");
    delay(2000);
    ESP.restart();
  }

  // Success path: print assigned IP for debugging.
  Serial.println("\nWiFi connected!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
}
