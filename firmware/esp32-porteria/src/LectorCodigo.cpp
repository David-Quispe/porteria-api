#include "LectorCodigo.h"

#include "config.h"

namespace {

HardwareSerial gm65(2);
String trama;
bool descartando = false;

bool interpretar(const String& texto, CodigoLeido& codigo) {
    String valor = texto;
    valor.trim();
    if (valor.length() == 0) {
        return false;
    }

    // El QR con prefijo evita la ambigüedad con un DNI de ocho dígitos.
    if (valor.startsWith("QR:")) {
        valor.remove(0, 3);
        if (valor.length() == 0 || valor.length() > 100) return false;
        codigo.metodo = "QR";
    } else {
        if (valor.length() > 100) return false;
        bool dni = valor.length() == 8;
        for (unsigned i = 0; dni && i < valor.length(); ++i) {
            dni = valor[i] >= '0' && valor[i] <= '9';
        }
        codigo.metodo = dni ? "DNI" : "QR";
    }
    codigo.valor = valor;
    return true;
}

}  // namespace

void LectorCodigo::iniciar() {
    gm65.begin(BAUDIOS_GM65, SERIAL_8N1, PIN_GM65_RX, PIN_GM65_TX);
    Serial.println("GM65 listo: UART 9600, salida CR o CRLF");
}

bool LectorCodigo::leer(CodigoLeido& codigo) {
    while (gm65.available()) {
        char c = static_cast<char>(gm65.read());
        if (c == '\r' || c == '\n') {
            if (descartando) {
                descartando = false;
                trama = "";
                continue;
            }
            if (trama.length() == 0) continue;  // Segundo byte de CRLF.
            String texto = trama;
            trama = "";
            if (interpretar(texto, codigo)) return true;
            Serial.println("Código QR/DNI vacío o demasiado largo");
        } else if (!descartando) {
            if (trama.length() >= 103) {
                descartando = true;
                trama = "";
                Serial.println("Trama GM65 demasiado larga; se descarta");
            } else {
                trama += c;
            }
        }
    }
    return false;
}
