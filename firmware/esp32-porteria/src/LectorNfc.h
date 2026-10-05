#pragma once

#include <Arduino.h>

// Lector PN532 por SPI. Lee el UID de stickers NFC (ISO14443A: NTAG, MIFARE).
namespace LectorNfc {

// false si no responde el PN532: revisar cableado y que los switches estén en modo SPI.
bool iniciar();

// true si hay un sticker en el lector. El UID sale en hex mayúsculas sin separadores,
// que es el formato que la API guarda. Espera como máximo ~100 ms, así que no bloquea el loop.
bool leerUid(String& uid);

}  // namespace LectorNfc
