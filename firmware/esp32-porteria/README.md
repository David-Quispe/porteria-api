# Firmware ESP32 — Portería (v1)

Lee stickers NFC con un PN532, envía cada lectura a la API y responde con LEDs y buzzer.
El contrato con la API está en [docs/decisiones-fase0.md](../../docs/decisiones-fase0.md) §5.

La v1 solo lee NFC. QR y DNI (lector GM65), el botón de entrada/salida y el servo de barrera llegan en la v2 (A10).

## Materiales

- ESP32 DevKit V1
- Módulo PN532 (placa roja V3) y stickers NFC (NTAG213/215 o MIFARE)
- LED verde, rojo y ámbar, con resistencias de 220 Ω
- Buzzer activo (si es pasivo, poner `BUZZER_ACTIVO = false` en `include/config.h`)

## Cableado

El PN532 va por **SPI**. En el módulo, los switches deben quedar así: **1 = OFF, 2 = ON**.

| PN532 | ESP32 |
| --- | --- |
| VCC | 3V3 |
| GND | GND |
| SCK | GPIO 18 |
| MISO | GPIO 19 |
| MOSI | GPIO 23 |
| SS (NSS) | GPIO 5 |

| Componente | ESP32 |
| --- | --- |
| LED verde (+ resistencia) | GPIO 25 |
| LED rojo (+ resistencia) | GPIO 26 |
| LED ámbar (+ resistencia) | GPIO 27 |
| Buzzer (+) | GPIO 14 |
| Cátodos de los LED y (−) del buzzer | GND |

Para cambiar algún pin, se edita `include/config.h`.

## Configurar y subir

1. Copiar `include/secrets.example.h` como `include/secrets.h` y completar el WiFi, la URL de la API y el token. `secrets.h` no se sube a Git.
2. En `API_BASE_URL` va la **IP de la laptop** que corre la API (`ipconfig` → Dirección IPv4), nunca `localhost`. El ESP32 y la laptop tienen que estar en la misma red WiFi, con el puerto 8080 abierto en el firewall de Windows.
3. El token se obtiene al registrar el dispositivo en la API (tarea A6). Hasta entonces, el ESP32 recibe 401/403.
4. Compilar y subir con los botones de PlatformIO en VS Code, o desde esta carpeta:

```bash
pio run -t upload
```

```bash
pio device monitor
```

## Qué significa cada señal

| Señal | Significado |
| --- | --- |
| Verde, ámbar y rojo en secuencia + pitido al encender | Prueba de cableado |
| Verde + 1 pitido corto | AUTORIZADO |
| Rojo + 2 pitidos | Cualquier otro resultado, o la API rechazó la petición |
| Ámbar fijo | Sin WiFi; el circuito reintenta solo |
| Ámbar parpadeando 3 veces | La API no respondió en 3 s, o rechazó el token |

## Comportamiento

- Si un sticker queda apoyado en el lector no se reenvía: tiene que retirarse 3 s para que cuente como una lectura nueva. La API además ignora lecturas repetidas en menos de 5 s.
- Envía un ping cada 60 s para que la API sepa que el dispositivo sigue vivo.
- Si el PN532 no responde al arrancar, lo reintenta cada 5 s y avisa por Serial.
- Nunca se cuelga sin WiFi: sigue leyendo y avisa con ámbar.

## Probar antes de que exista la API completa

- **El lector:** abrir el monitor Serial y acercar un sticker. Se imprime `NFC 04A1B2C3D4E5F6 -> ...`. Ese UID es el que se registra como credencial (tarea B5).
- **Los LED y el buzzer:** la secuencia de arranque prende los tres LED y suena una vez.
- **La API:** hasta que existan A3 y A5, la API responde 403 y el ESP32 lo muestra como token inválido (ámbar). Es lo esperado.

## Problemas frecuentes

| Síntoma | Solución |
| --- | --- |
| `No responde el PN532` | Revisar los switches (1 OFF, 2 ON), que MISO y MOSI no estén cruzados, y que VCC sea 3V3 |
| `la API no respondió a tiempo` | `API_BASE_URL` con la IP correcta, misma red WiFi y puerto 8080 permitido en el firewall |
| `401/403: token inválido` | Volver a copiar el token en `DEVICE_TOKEN`; si se regeneró en la API, el anterior ya no sirve |
| No compila: `Falta include/secrets.h` | Copiar `secrets.example.h` como `secrets.h` |
