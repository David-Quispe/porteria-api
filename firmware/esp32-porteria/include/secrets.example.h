#pragma once

// Copiar este archivo como secrets.h (que NO se sube a Git) y completar.

#define WIFI_SSID "nombre-de-la-red"
#define WIFI_PASSWORD "contraseña-de-la-red"

// IP de la laptop que corre la API (ipconfig → Dirección IPv4), nunca "localhost":
// para el ESP32, localhost es él mismo. Misma red WiFi y puerto 8080 abierto en el firewall.
#define API_BASE_URL "http://192.168.1.50:8080"

// Token que devuelve la API al registrar el dispositivo. Se muestra una sola vez.
#define DEVICE_TOKEN "pegar-aqui-el-token-del-dispositivo"
