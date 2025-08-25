#include <Arduino.h>
#include "button_module.h"

// Input pin for the button; assumes external button wired to GND.
#define BUTTON_PIN  20

// Debounced edge detection state: remember last sampled level.
static int lastButtonState = HIGH;

// Function called in Setup()
void initButton() {
  // Enable internal pull-up; expect the button to short to GND when pressed.
  pinMode(BUTTON_PIN, INPUT_PULLUP);
}

// Used to signal when the ESP32 should take a pciture
bool buttonPressed() {
  // Read instantaneous level and compare with last known state.
  int s = digitalRead(BUTTON_PIN);

  // Detect falling edge: HIGH (idle) → LOW (pressed).
  if (s == LOW && lastButtonState == HIGH) {
      lastButtonState = s;
      delay(50);            // Basic debounce; blocks briefly
      return true;          // Report a single press event
  }

  // Update state and report no new press.
  lastButtonState = s;
  return false;
}
