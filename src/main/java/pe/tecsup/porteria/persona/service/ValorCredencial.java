package pe.tecsup.porteria.persona.service;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import pe.tecsup.porteria.persona.entity.TipoCredencial;
import pe.tecsup.porteria.shared.exception.BusinessException;
public final class ValorCredencial {
    private ValorCredencial() {}
    public static String normalizar(TipoCredencial tipo, String valor) {
        if (tipo != TipoCredencial.NFC) return valor;
        String uid = valor.replaceAll("[:\\s]", "").toUpperCase(Locale.ROOT);
        if (!uid.matches("(?:[0-9A-F]{8}|[0-9A-F]{14}|[0-9A-F]{20})")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El UID NFC debe contener 4, 7 o 10 bytes hexadecimales");
        }
        return uid;
    }
}
