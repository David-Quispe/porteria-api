package pe.tecsup.porteria.acceso.entity;

/**
 * Cómo se identificó la persona en el dispositivo.
 * NFC y QR se buscan como credencial; DNI se busca directo en la persona.
 */
public enum MetodoId {
    NFC,
    QR,
    DNI
}
