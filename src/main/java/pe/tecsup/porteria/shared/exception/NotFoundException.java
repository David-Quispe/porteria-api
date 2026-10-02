package pe.tecsup.porteria.shared.exception;

/**
 * El recurso pedido no existe. Se responde con 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String recurso, Object id) {
        this(recurso + " con id " + id + " no existe");
    }
}
