// Firmware v2: NFC, QR y DNI; la API es la única que decide si abrir.

#include <Arduino.h>
#include <WiFi.h>
#include <ESP32Servo.h>

#include "ApiPorteria.h"
#include "Indicadores.h"
#include "LectorNfc.h"
#include "LectorCodigo.h"
#include "config.h"

namespace {

bool lectorListo = false;
String ultimoUid;
uint32_t ultimaLecturaMs = 0;
uint32_t ultimoPingMs = 0;
uint32_t ultimoReintentoWifiMs = 0;
uint32_t ultimoReintentoLectorMs = 0;
String ultimoCodigo;
uint32_t ultimoCodigoMs = 0;
const char* direccion = DIRECCION_INICIAL;
Servo barrera;
uint32_t cierreBarreraMs = 0;
bool barreraAbierta = false;
bool botonCrudoAnterior = HIGH;
bool botonEstable = HIGH;
uint32_t ultimoCambioBotonMs = 0;
bool direccionSalida = false;

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
    if (!conectado && barreraAbierta) {
        barrera.write(ANGULO_BARRERA_CERRADA);
        barreraAbierta = false;
    }
    Indicadores::sinConexion(!conectado);
    if (!conectado && millis() - ultimoReintentoWifiMs > INTERVALO_REINTENTO_MS) {
        ultimoReintentoWifiMs = millis();
        WiFi.reconnect();
    }
}

void vigilarBarrera() {
    if (barreraAbierta && static_cast<int32_t>(millis() - cierreBarreraMs) >= 0) {
        barrera.write(ANGULO_BARRERA_CERRADA);
        barreraAbierta = false;
    }
}

void vigilarBoton() {
    bool actual = digitalRead(PIN_DIRECCION);
    if (actual != botonCrudoAnterior) {
        ultimoCambioBotonMs = millis();
        botonCrudoAnterior = actual;
    }
    if (actual != botonEstable && millis() - ultimoCambioBotonMs >= REBOTE_BOTON_MS) {
        botonEstable = actual;
        if (botonEstable == LOW) {
            direccionSalida = !direccionSalida;
            direccion = direccionSalida ? "SALIDA" : DIRECCION_INICIAL;
            Serial.printf("Dirección: %s\n", direccion);
        }
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

void procesarLectura(const char* metodo, const String& valor) {
    // Un DNI o valor QR es un dato personal; no imprimirlo completo por Serial.
    Serial.printf("%s (%s) -> ", metodo, direccion);
    if (!hayWifi()) {
        Serial.println("sin WiFi");
        Indicadores::errorDeComunicacion();
        return;
    }

    uint32_t inicio = millis();
    RespuestaLectura respuesta = ApiPorteria::enviarLectura(metodo, valor, direccion);
    uint32_t demora = millis() - inicio;

    switch (respuesta.estado) {
        case RespuestaLectura::Estado::OK:
            Serial.printf("%s %s (%lu ms)\n", respuesta.resultado.c_str(), respuesta.nombre.c_str(),
                          static_cast<unsigned long>(demora));
            if (respuesta.abrir) {
                barrera.write(ANGULO_BARRERA_ABIERTA);
                barreraAbierta = true;
                cierreBarreraMs = millis() + DURACION_BARRERA_MS;
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
    Serial.println("\n== Portería ESP32 v2 ==");
    Indicadores::iniciar();
    Indicadores::probar();
    pinMode(PIN_DIRECCION, INPUT_PULLUP);
    barrera.attach(PIN_SERVO);
    barrera.write(ANGULO_BARRERA_CERRADA);
    LectorCodigo::iniciar();

    lectorListo = LectorNfc::iniciar();
    if (!lectorListo) {
        Serial.println("No responde el PN532: revisa el cableado y que los switches estén en modo SPI");
    }
    conectarWifi();
}

void loop() {
    vigilarWifi();
    vigilarBarrera();
    vigilarBoton();

    if (!lectorListo) {
        if (millis() - ultimoReintentoLectorMs > INTERVALO_REINTENTO_MS) {
            ultimoReintentoLectorMs = millis();
            lectorListo = LectorNfc::iniciar();
            if (!lectorListo) {
                Indicadores::errorDeComunicacion();
            }
        }
    }

    enviarPingSiToca();

    String uid;
    if (lectorListo && LectorNfc::leerUid(uid)) {
        // Mientras el sticker permanezca apoyado, cada lectura renueva la espera.
        bool mismoSticker = uid == ultimoUid && millis() - ultimaLecturaMs < ESPERA_MISMA_LECTURA_MS;
        ultimoUid = uid;
        ultimaLecturaMs = millis();
        if (!mismoSticker) procesarLectura("NFC", uid);
    }

    CodigoLeido codigo;
    if (LectorCodigo::leer(codigo)) {
        String clave = String(codigo.metodo) + ':' + codigo.valor;
        bool repetido = clave == ultimoCodigo && millis() - ultimoCodigoMs < ESPERA_MISMA_LECTURA_MS;
        ultimoCodigo = clave;
        ultimoCodigoMs = millis();
        if (!repetido) procesarLectura(codigo.metodo, codigo.valor);
    }
}
