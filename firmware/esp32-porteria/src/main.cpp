// Firmware v1 del circuito de portería: lee stickers NFC con el PN532,
// pregunta a la API y responde con LEDs y buzzer.

#include <Arduino.h>
#include <WiFi.h>

#include "ApiPorteria.h"
#include "Indicadores.h"
#include "LectorNfc.h"
#include "config.h"

namespace {

bool lectorListo = false;
String ultimoUid;
uint32_t ultimaLecturaMs = 0;
uint32_t ultimoPingMs = 0;
uint32_t ultimoReintentoWifiMs = 0;
uint32_t ultimoReintentoLectorMs = 0;

bool hayWifi() {
    return WiFi.status() == WL_CONNECTED;
}

void conectarWifi() {
    Serial.printf("Conectando a %s", WIFI_SSID);
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    uint32_t inicio = millis();
    while (!hayWifi() && millis() - inicio < 10000) {
        Serial.print('.');
        delay(500);
    }
    if (hayWifi()) {
        Serial.printf("\nWiFi OK, IP %s\n", WiFi.localIP().toString().c_str());
    } else {
        Serial.println("\nSin WiFi; se reintenta en segundo plano");
    }
}

// Ámbar fijo y reintento periódico mientras no haya WiFi. Nunca bloquea.
void vigilarWifi() {
    bool conectado = hayWifi();
    Indicadores::sinConexion(!conectado);
    if (!conectado && millis() - ultimoReintentoWifiMs > INTERVALO_REINTENTO_MS) {
        ultimoReintentoWifiMs = millis();
        WiFi.reconnect();
    }
}

void enviarPingSiToca() {
    if (!hayWifi() || millis() - ultimoPingMs < INTERVALO_PING_MS) {
        return;
    }
    ultimoPingMs = millis();
    int codigo = ApiPorteria::ping();
    if (codigo != 200) {
        Serial.printf("Ping a la API: %d\n", codigo);
    }
}

void procesarLectura(const String& uid) {
    Serial.printf("NFC %s -> ", uid.c_str());
    if (!hayWifi()) {
        Serial.println("sin WiFi");
        Indicadores::errorDeComunicacion();
        return;
    }

    uint32_t inicio = millis();
    RespuestaLectura respuesta = ApiPorteria::enviarLectura("NFC", uid, DIRECCION);
    uint32_t demora = millis() - inicio;

    switch (respuesta.estado) {
        case RespuestaLectura::Estado::OK:
            Serial.printf("%s %s (%lu ms)\n", respuesta.resultado.c_str(), respuesta.nombre.c_str(),
                          static_cast<unsigned long>(demora));
            if (respuesta.abrir) {
                Indicadores::autorizado();
            } else {
                Indicadores::denegado();
            }
            break;
        case RespuestaLectura::Estado::TOKEN_INVALIDO:
            Serial.println("401/403: token inválido, dispositivo desactivado o ruta no habilitada (revisa DEVICE_TOKEN)");
            Indicadores::errorDeComunicacion();
            break;
        case RespuestaLectura::Estado::PETICION_INVALIDA:
            Serial.println("la API rechazó la petición (4xx)");
            Indicadores::denegado();
            break;
        case RespuestaLectura::Estado::SIN_RESPUESTA:
            Serial.println("la API no respondió a tiempo");
            Indicadores::errorDeComunicacion();
            break;
    }
}

}  // namespace

void setup() {
    Serial.begin(115200);
    Serial.println("\n== Portería ESP32 v1 ==");
    Indicadores::iniciar();
    Indicadores::probar();

    lectorListo = LectorNfc::iniciar();
    if (!lectorListo) {
        Serial.println("No responde el PN532: revisa el cableado y que los switches estén en modo SPI");
    }
    conectarWifi();
}

void loop() {
    vigilarWifi();

    if (!lectorListo) {
        if (millis() - ultimoReintentoLectorMs > INTERVALO_REINTENTO_MS) {
            ultimoReintentoLectorMs = millis();
            lectorListo = LectorNfc::iniciar();
            if (!lectorListo) {
                Indicadores::errorDeComunicacion();
            }
        }
        return;
    }

    enviarPingSiToca();

    String uid;
    if (!LectorNfc::leerUid(uid)) {
        return;
    }

    // Mientras el mismo sticker siga apoyado, cada lectura renueva la espera: no se reenvía.
    bool mismoSticker = uid == ultimoUid && millis() - ultimaLecturaMs < ESPERA_MISMA_LECTURA_MS;
    ultimoUid = uid;
    ultimaLecturaMs = millis();
    if (mismoSticker) {
        return;
    }

    procesarLectura(uid);
    ultimaLecturaMs = millis();
}
