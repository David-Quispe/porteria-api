package pe.tecsup.porteria.shared.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Cuerpo único de error de toda la API. El frontend y el ESP32 siempre reciben esta forma.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(HttpStatusCode status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ApiError of(HttpStatusCode status, String message, String path, List<FieldError> errors) {
        return new ApiError(LocalDateTime.now(), status.value(), reasonPhrase(status), message, path, errors);
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : String.valueOf(status.value());
    }
}
