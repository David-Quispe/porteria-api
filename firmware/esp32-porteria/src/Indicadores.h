#pragma once

// LEDs y buzzer. Las señales de resultado duran ~1.5 s y bloquean:
// mientras se muestran no se lee otra credencial.
namespace Indicadores {

void iniciar();

// Prueba de arranque: prende cada LED y suena el buzzer para revisar el cableado de un vistazo.
void probar();

void autorizado();
void denegado();

// La API no respondió, o rechazó el token: tres parpadeos ámbar con pitido.
void errorDeComunicacion();

// Ámbar fijo mientras no haya WiFi.
void sinConexion(bool activo);

}  // namespace Indicadores
