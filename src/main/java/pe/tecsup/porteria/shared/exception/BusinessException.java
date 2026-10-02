package pe.tecsup.porteria.shared.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Se rompió una regla de negocio. Por defecto 409 (conflicto, p. ej. DNI repetido);
 * usar 422 cuando el dato es válido en forma pero no tiene sentido (p. ej. rango de fechas invertido).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.CONFLICT, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
