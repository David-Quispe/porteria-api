#include "Indicadores.h"

#include "config.h"

namespace {

void sonar(uint16_t ms) {
    if (BUZZER_ACTIVO) {
        digitalWrite(PIN_BUZZER, HIGH);
        delay(ms);
        digitalWrite(PIN_BUZZER, LOW);
    } else {
        tone(PIN_BUZZER, 2000, ms);
        delay(ms);
    }
}

// Prende un LED, suena n pitidos y mantiene el LED hasta completar DURACION_SENAL_MS.
void senal(uint8_t pinLed, uint8_t pitidos, uint16_t msPitido) {
    uint32_t inicio = millis();
    digitalWrite(pinLed, HIGH);
    for (uint8_t i = 0; i < pitidos; i++) {
        sonar(msPitido);
        delay(80);
    }
    uint32_t transcurrido = millis() - inicio;
    if (transcurrido < DURACION_SENAL_MS) {
        delay(DURACION_SENAL_MS - transcurrido);
    }
    digitalWrite(pinLed, LOW);
}

}  // namespace

void Indicadores::iniciar() {
    for (uint8_t pin : {PIN_LED_VERDE, PIN_LED_ROJO, PIN_LED_AMBAR, PIN_BUZZER}) {
        pinMode(pin, OUTPUT);
        digitalWrite(pin, LOW);
    }
}

void Indicadores::probar() {
    for (uint8_t pin : {PIN_LED_VERDE, PIN_LED_ROJO, PIN_LED_AMBAR}) {
        digitalWrite(pin, HIGH);
        delay(250);
        digitalWrite(pin, LOW);
    }
    sonar(100);
}

void Indicadores::autorizado() {
    senal(PIN_LED_VERDE, 1, 120);
}

void Indicadores::denegado() {
    senal(PIN_LED_ROJO, 2, 250);
}

void Indicadores::errorDeComunicacion() {
    for (uint8_t i = 0; i < 3; i++) {
        digitalWrite(PIN_LED_AMBAR, HIGH);
        sonar(80);
        delay(170);
        digitalWrite(PIN_LED_AMBAR, LOW);
        delay(250);
    }
}

void Indicadores::sinConexion(bool activo) {
    digitalWrite(PIN_LED_AMBAR, activo ? HIGH : LOW);
}
