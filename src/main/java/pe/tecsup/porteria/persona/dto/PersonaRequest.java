package pe.tecsup.porteria.persona.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.persona.entity.TipoPersona;
@Schema(example = "{\"tipo\":\"ESTUDIANTE\",\"dni\":\"90000001\",\"nombres\":\"Lucia\",\"apellidos\":\"Quispe\",\"codigo\":\"DEM-001\",\"area\":\"Ingeniería de sistemas\",\"vigenciaInicio\":\"2026-01-01\"}" )
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
