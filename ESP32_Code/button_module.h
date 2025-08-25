#ifndef BUTTON_MODULE_H
#define BUTTON_MODULE_H

/*-----------------------------------------------------------------------------
  Module: Button
  Purpose: Initialize a pull-up input and detect edge-triggered presses.
  Logic: Active-LOW button (pressed = LOW).
-----------------------------------------------------------------------------*/
void initButton();
bool buttonPressed();   // Returns true once per press (on the falling edge).

#endif
