package pe.tecsup.porteria.persona.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.persona.entity.TipoPersona;
public record PersonaRequest(
        @NotNull TipoPersona tipo, @NotBlank @Pattern(regexp = "[0-9]{8,15}") String dni,
        @NotBlank @Size(max=100) String nombres, @NotBlank @Size(max=100) String apellidos,
        @Size(max=20) String codigo, @Size(max=100) String area,
        @NotNull LocalDate vigenciaInicio, LocalDate vigenciaFin) {
    @AssertTrue(message = "La vigencia final no puede ser anterior a la inicial")
    public boolean isVigenciaValida() {
        return vigenciaInicio == null || vigenciaFin == null || !vigenciaFin.isBefore(vigenciaInicio);
    }
}
