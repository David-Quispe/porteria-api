package pe.tecsup.porteria.acceso.entity;

/**
 * Sentido del paso. Lo envía siempre el ESP32. Las reglas de horario solo se aplican a la ENTRADA.
 */
public enum Direccion {
    ENTRADA,
    SALIDA
}
