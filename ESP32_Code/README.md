# IoT Image Processing and Feedback System

This project demonstrates an **end-to-end IoT pipeline** combining an ESP32-S3 camera module with a Spring Boot backend service.  
The device captures images, uploads them to the server for processing (e.g., OCR or classification), and receives back a simple response that is conveyed to the user via **tactile vibration feedback**.

---

## Features

- **ESP32-S3 Firmware**
  - Camera initialization and image capture
  - Wi-Fi connectivity with reconnect and error handling
  - HTTP POST of captured JPEG images to the server
  - Button interface to trigger capture
  - Vibration motor driver for haptic feedback

- **Spring Boot Backend**
  - REST API endpoint `/upload` for receiving images
  - Integration with OCR/LLM (via OpenAI or other providers)
  - JSON parsing, validation, and result extraction
  - Persistence with Spring Data JPA and MySQL
  - Health check endpoint `/ping`

- **Feedback Loop**
  - Server maps classification results (A–D) to an action code
  - Device translates action codes to vibration patterns (1–4 pulses)

---

## Tech Stack

- **Embedded**: ESP32-S3, Arduino framework (C++), esp32-camera, WiFiClient, HTTPClient  
- **Backend**: Java 17, Spring Boot 3.x, Spring Data JPA, MySQL, OkHttp, Jackson  
- **Communication**: HTTP/1.1, JSON over REST, image/jpeg payloads  
- **Testing**: JUnit 5, WebTestClient, parameterized integration tests  
