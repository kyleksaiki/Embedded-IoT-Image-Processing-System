#ifndef SERVER_H
#define SERVER_H

/*-----------------------------------------------------------------------------
  Module: Server/Transport
  Purpose: Capture a frame and POST to server; parse 1-char action code.
  Contract:
    - Assumes Wi-Fi is already connected.
    - Returns enum ActionResult mapping to haptic feedback decisions.
  Response:
    - Body must be exactly one ASCII character: 'A' | 'B' | 'C' | 'D'.
    - Non-200 or unexpected body → ACTION_ERROR.
-----------------------------------------------------------------------------*/

enum ActionResult { ACTION_A, ACTION_B, ACTION_C, ACTION_D, ACTION_ERROR };

ActionResult captureAndSend();  // assumes Wi-Fi is connected

#endif
