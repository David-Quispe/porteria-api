#include "LectorNfc.h"

#include <Adafruit_PN532.h>
#include <SPI.h>

#include "config.h"

namespace {

constexpr uint16_t TIMEOUT_LECTURA_MS = 100;

Adafruit_PN532 nfc(PIN_PN532_SS);

}  // namespace

bool LectorNfc::iniciar() {
    if (!nfc.begin()) {
        return false;
    }
    uint32_t version = nfc.getFirmwareVersion();
    if (version == 0) {
        return false;
    }
    Serial.printf("PN532 listo (firmware %u.%u)\n",
                  static_cast<unsigned>((version >> 16) & 0xFF),
                  static_cast<unsigned>((version >> 8) & 0xFF));
    nfc.SAMConfig();
    return true;
}

bool LectorNfc::leerUid(String& uid) {
    uint8_t bytes[10];
    uint8_t largo = 0;
    if (!nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, bytes, &largo, TIMEOUT_LECTURA_MS)) {
        return false;
    }
    uid = "";
    for (uint8_t i = 0; i < largo; i++) {
        char hex[3];
        snprintf(hex, sizeof(hex), "%02X", bytes[i]);
        uid += hex;
    }
    return true;
}
