package pe.tecsup.porteria.persona.dto;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.persona.entity.TipoCredencial;
public record CredencialRequest(@NotNull TipoCredencial tipo, @NotBlank @Size(max=100) String valor) {}
