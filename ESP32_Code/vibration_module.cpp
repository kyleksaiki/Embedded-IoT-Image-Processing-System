#include <Arduino.h>
#include "vibration_module.h"

// GPIO that drives the vibration motor (directly or via a transistor/MOSFET).
// Ensure the motor is not driven directly from a GPIO if current > 12mA.
// Use a driver transistor, 1 k Ohm resitor, and flyback diode 
#define BUZZER_PIN  14

// Function called in setup
void initVibration() {
    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);   // Start with motor off.
}

// Vibrates for 200 ms n amount of times
void vibratePulses(int count) {
    for (int i = 0; i < count; i++) {
        digitalWrite(BUZZER_PIN, HIGH);
        delay(200);
        digitalWrite(BUZZER_PIN, LOW);
        delay(200);
    }
}
