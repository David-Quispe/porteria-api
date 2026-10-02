package pe.tecsup.porteria.shared.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Respuesta paginada estable. Nunca se devuelve un {@code Page} de Spring directamente:
 * su JSON cambia entre versiones.
 * <p>
 * Uso: {@code PageResponse.of(repository.findAll(spec, pageable).map(mapper::toResponse))}
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
