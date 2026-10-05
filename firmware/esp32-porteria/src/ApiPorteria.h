#pragma once

#include <Arduino.h>

// Cliente de la API. El contrato está en docs/decisiones-fase0.md §5.

struct RespuestaLectura {
    enum class Estado {
        OK,                 // 200: la API decidió; mirar `abrir`
        TOKEN_INVALIDO,     // 401/403: token falso, dispositivo desactivado o ruta no habilitada
        PETICION_INVALIDA,  // otro 4xx: la API rechazó el JSON
        SIN_RESPUESTA       // sin WiFi, timeout, 5xx o respuesta ilegible
    };

    Estado estado = Estado::SIN_RESPUESTA;
    bool abrir = false;
    String resultado;
    String nombre;
};

namespace ApiPorteria {

RespuestaLectura enviarLectura(const char* metodo, const String& valor, const char* direccion);

// Avisa a la API que el dispositivo sigue vivo. Devuelve el código HTTP (negativo si no hubo respuesta).
int ping();

}  // namespace ApiPorteria
