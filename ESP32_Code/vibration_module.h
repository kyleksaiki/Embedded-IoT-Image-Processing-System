#ifndef VIBRATION_MODULE_H
#define VIBRATION_MODULE_H

/*-----------------------------------------------------------------------------
  Module: Vibration/Haptics
  Purpose: Initialize motor driver pin and emit simple pulse patterns.
  Contract:
    - initVibration() must be called once at startup.
    - vibratePulses(count) blocks and toggles output for count pulses.
-----------------------------------------------------------------------------*/
void initVibration();
void vibratePulses(int count);

#endif
