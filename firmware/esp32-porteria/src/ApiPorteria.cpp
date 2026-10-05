#include "ApiPorteria.h"

#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <WiFi.h>

#include "config.h"

namespace {

// POST con el token del dispositivo. Devuelve el código HTTP, o un número negativo si no hubo respuesta.
int post(const char* ruta, const String& cuerpo, String& respuesta) {
    if (WiFi.status() != WL_CONNECTED) {
        return -1;
    }
    HTTPClient http;
    http.setConnectTimeout(TIMEOUT_HTTP_MS);
    http.setTimeout(static_cast<uint16_t>(TIMEOUT_HTTP_MS));
    if (!http.begin(String(API_BASE_URL) + ruta)) {
        return -1;
    }
    http.addHeader("Content-Type", "application/json");
    http.addHeader("X-Device-Token", DEVICE_TOKEN);
    int codigo = http.POST(cuerpo);
    if (codigo > 0) {
        respuesta = http.getString();
    }
    http.end();
    return codigo;
}

}  // namespace

RespuestaLectura ApiPorteria::enviarLectura(const char* metodo, const String& valor, const char* direccion) {
    JsonDocument peticion;
    peticion["metodo"] = metodo;
    peticion["valor"] = valor;
    peticion["direccion"] = direccion;
    String cuerpo;
    serializeJson(peticion, cuerpo);

    String texto;
    int codigo = post("/api/dispositivo/lecturas", cuerpo, texto);

    RespuestaLectura respuesta;
    if (codigo == 401 || codigo == 403) {
        respuesta.estado = RespuestaLectura::Estado::TOKEN_INVALIDO;
        return respuesta;
    }
    if (codigo >= 400 && codigo < 500) {
        respuesta.estado = RespuestaLectura::Estado::PETICION_INVALIDA;
        return respuesta;
    }
    if (codigo != 200) {
        return respuesta;  // SIN_RESPUESTA
    }

    JsonDocument json;
    if (deserializeJson(json, texto)) {
        return respuesta;  // SIN_RESPUESTA: llegó algo que no es el JSON del contrato
    }
    respuesta.estado = RespuestaLectura::Estado::OK;
    respuesta.abrir = json["abrir"] | false;
    respuesta.resultado = json["resultado"] | "";
    respuesta.nombre = json["nombre"] | "";
    return respuesta;
}

int ApiPorteria::ping() {
    String ignorada;
    return post("/api/dispositivo/ping", "", ignorada);
}
