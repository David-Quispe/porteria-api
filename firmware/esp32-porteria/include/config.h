#pragma once

#include <Arduino.h>

// ---------------------------------------------------------------------
// Configuración del circuito. Lo secreto o propio de cada red (WiFi,
// IP de la API, token) va en secrets.h, que no se sube a Git.
// ---------------------------------------------------------------------

#if __has_include("secrets.h")
#include "secrets.h"
#else
#error "Falta include/secrets.h: copia include/secrets.example.h como include/secrets.h y complétalo"
#endif

// Pines (ESP32 DevKit V1). El PN532 va por SPI: SCK=18, MISO=19, MOSI=23 y SS abajo.
constexpr uint8_t PIN_PN532_SS = 5;
constexpr uint8_t PIN_LED_VERDE = 25;
constexpr uint8_t PIN_LED_ROJO = 26;
constexpr uint8_t PIN_LED_AMBAR = 27;
constexpr uint8_t PIN_BUZZER = 14;

// true: buzzer activo (suena solo con HIGH). false: buzzer pasivo (necesita una frecuencia).
constexpr bool BUZZER_ACTIVO = true;

// En la v1 la dirección es fija por dispositivo; en la v2 la elige un botón.
constexpr const char* DIRECCION = "ENTRADA";

// Contrato con la API: sin respuesta en 3 s se considera caída.
constexpr uint32_t TIMEOUT_HTTP_MS = 3000;
// Un sticker tiene que retirarse este tiempo antes de que vuelva a contar como lectura nueva.
constexpr uint32_t ESPERA_MISMA_LECTURA_MS = 3000;
constexpr uint32_t INTERVALO_PING_MS = 60000;
constexpr uint32_t INTERVALO_REINTENTO_MS = 5000;
constexpr uint32_t DURACION_SENAL_MS = 1500;
