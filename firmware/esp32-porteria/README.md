# Firmware ESP32 — Portería (v2)

Lee stickers NFC con un PN532 y códigos QR o DNI con un GM65. Envía cada lectura a la API y responde con LEDs, buzzer y servo.
El contrato con la API está en [docs/decisiones-fase0.md](../../docs/decisiones-fase0.md) §5.

El firmware implementa A9 y A10. La prueba de tiempos, apertura y lecturas con el circuito real sigue siendo necesaria.

## Materiales

- ESP32 DevKit V1
- Módulo PN532 (placa roja V3) y stickers NFC (NTAG213/215 o MIFARE)
- GM65 con salida UART TTL configurada a 9600 baudios, terminador CR o CRLF y lectura de QR y Code39
- Pulsador de entrada/salida y servo de barrera con **alimentación externa adecuada**
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
| GM65 TX | GPIO 16 (RX2) |
| GM65 RX | GPIO 17 (TX2, opcional) |
| Pulsador | GPIO 32 y GND |
| Señal del servo | GPIO 13 |
| Cátodos de los LED y (−) del buzzer | GND |

Conecta las tierras de la alimentación del servo y del ESP32. No alimentes el servo desde el pin 3V3 del ESP32. Comprueba la tensión TTL de salida del GM65 antes de conectar su TX al GPIO 16; si sale a 5 V, usa un adaptador de nivel a 3,3 V. La alimentación del GM65 debe seguir las especificaciones de tu módulo. Para cambiar pines, ángulos o tiempo de apertura, edita `include/config.h`.

## Configurar y subir

1. Copiar `include/secrets.example.h` como `include/secrets.h` y completar el WiFi, la URL de la API y el token. `secrets.h` no se sube a Git.
2. En `API_BASE_URL` va la **IP de la laptop** que corre la API (`ipconfig` → Dirección IPv4), nunca `localhost`. El ESP32 y la laptop tienen que estar en la misma red WiFi, con el puerto 8080 abierto en el firewall de Windows.
3. El token se obtiene al registrar el dispositivo en la API (tarea A6). Hasta entonces, el ESP32 recibe 401/403.
4. Configura el GM65 para salida UART sin prefijos de protocolo, con terminador CR o CRLF y Code39 habilitado. Un Code39 con ocho dígitos se interpreta como DNI. Los demás códigos se interpretan como QR. Si el valor de un QR son exactamente ocho dígitos, codifica `QR:12345678`; el prefijo no se envía a la API. Registra en la API el valor sin `QR:`.
5. Compilar y subir con los botones de PlatformIO en VS Code, o desde esta carpeta:

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
| Servo abierto durante 3 s | La API respondió `abrir: true`; después vuelve a posición cerrada |
| Pulsar el botón | Alterna ENTRADA y SALIDA; la dirección actual aparece en el monitor Serial |

## Comportamiento

- Si un sticker queda apoyado en el lector no se reenvía: tiene que retirarse 3 s para que cuente como una lectura nueva. La API además ignora lecturas repetidas en menos de 5 s.
- Envía un ping cada 60 s para que la API sepa que el dispositivo sigue vivo.
- Si el PN532 no responde al arrancar, lo reintenta cada 5 s y avisa por Serial.
- Nunca se cuelga sin WiFi: sigue leyendo y avisa con ámbar.
- Sin PN532 sigue aceptando QR/DNI del GM65 y reintenta el PN532 en segundo plano.
- Si se pierde WiFi mientras la barrera está abierta, el servo vuelve a la posición cerrada.

## Prueba de aceptación con el circuito

- **NFC:** registra un sticker, acércalo y confirma el resultado, el LED y el registro en la API.
- **QR:** registra una credencial QR, escanéala y confirma el mismo flujo. Usa un QR prefijado con `QR:` para probar el caso numérico de ocho dígitos.
- **DNI:** imprime ocho dígitos en Code39, escanéalos y confirma que se envían como método DNI.
- **Dirección:** pulsa el botón, comprueba que el siguiente acceso aparece como SALIDA y vuelve a pulsarlo para ENTRADA.
- **Barrera y fallas:** comprueba apertura solo con AUTORIZADO, cierre a los 3 s, cierre al perder WiFi, aviso ámbar sin red y que la API no duplica lecturas continuas.
- **Los LED y el buzzer:** la secuencia de arranque prende los tres LED y suena una vez.

## Problemas frecuentes

| Síntoma | Solución |
| --- | --- |
| `No responde el PN532` | Revisar los switches (1 OFF, 2 ON), que MISO y MOSI no estén cruzados, y que VCC sea 3V3 |
| `la API no respondió a tiempo` | `API_BASE_URL` con la IP correcta, misma red WiFi y puerto 8080 permitido en el firewall |
| `401/403: token inválido` | Volver a copiar el token en `DEVICE_TOKEN`; si se regeneró en la API, el anterior ya no sirve |
| No compila: `Falta include/secrets.h` | Copiar `secrets.example.h` como `secrets.h` |
