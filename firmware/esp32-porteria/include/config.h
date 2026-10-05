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
constexpr uint8_t PIN_GM65_RX = 16;
constexpr uint8_t PIN_GM65_TX = 17;
constexpr uint8_t PIN_DIRECCION = 32;
constexpr uint8_t PIN_SERVO = 13;

// true: buzzer activo (suena solo con HIGH). false: buzzer pasivo (necesita una frecuencia).
constexpr bool BUZZER_ACTIVO = true;

constexpr const char* DIRECCION_INICIAL = "ENTRADA";
constexpr uint32_t REBOTE_BOTON_MS = 50;
constexpr uint32_t DURACION_BARRERA_MS = 3000;
constexpr int ANGULO_BARRERA_CERRADA = 0;
constexpr int ANGULO_BARRERA_ABIERTA = 90;
constexpr uint32_t BAUDIOS_GM65 = 9600;

// Contrato con la API: sin respuesta en 3 s se considera caída.
constexpr uint32_t TIMEOUT_HTTP_MS = 3000;
// Un sticker tiene que retirarse este tiempo antes de que vuelva a contar como lectura nueva.
constexpr uint32_t ESPERA_MISMA_LECTURA_MS = 3000;
constexpr uint32_t INTERVALO_PING_MS = 60000;
constexpr uint32_t INTERVALO_REINTENTO_MS = 5000;
constexpr uint32_t DURACION_SENAL_MS = 1500;
