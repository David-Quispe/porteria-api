package pe.tecsup.porteria.shared.dto;
import jakarta.validation.constraints.NotNull;
public record EstadoRequest(@NotNull Boolean activo) {}
