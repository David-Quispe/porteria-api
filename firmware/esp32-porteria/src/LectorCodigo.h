#pragma once

#include <Arduino.h>

struct CodigoLeido {
    const char* metodo;
    String valor;
};

namespace LectorCodigo {
void iniciar();
bool leer(CodigoLeido& codigo);
}
