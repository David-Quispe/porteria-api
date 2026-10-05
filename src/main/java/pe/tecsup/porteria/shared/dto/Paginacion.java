package pe.tecsup.porteria.shared.dto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import pe.tecsup.porteria.shared.exception.BusinessException;
public final class Paginacion {
    private Paginacion() {}
    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "page debe ser >= 0 y size debe estar entre 1 y 100");
        }
        return PageRequest.of(page, size, sort);
    }
}
