package pe.tecsup.porteria.acceso.entity;

/**
 * Resultado de validar una lectura. El orden de evaluación está en docs/decisiones-fase0.md §3.
 */
public enum Resultado {
    /** Pasa todas las validaciones. */
    AUTORIZADO,
    /** Nadie tiene esa credencial activa, o el DNI no está registrado. */
    NO_AUTORIZADO,
    /** La persona existe pero fue desactivada. */
    INACTIVO,
    /** Hoy está fuera de la vigencia de la persona. */
    VENCIDO,
    /** Ninguna regla de acceso cubre este día y hora (solo en ENTRADA). */
    FUERA_DE_HORARIO;

    /** Lo único que mira el ESP32 para abrir la barrera y prender el LED verde. */
    public boolean abre() {
        return this == AUTORIZADO;
    }
}
