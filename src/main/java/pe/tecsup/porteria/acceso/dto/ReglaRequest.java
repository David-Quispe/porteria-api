package pe.tecsup.porteria.acceso.dto;
import java.time.LocalTime;
import java.util.Set;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.persona.entity.TipoPersona;
import pe.tecsup.porteria.dispositivo.entity.Punto;
public record ReglaRequest(@NotNull TipoPersona tipoPersona, Punto punto,
        @NotEmpty Set<@Min(1) @Max(7) Integer> dias, @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin, @Size(max=150) String descripcion) {
    @AssertTrue(message="La hora final debe ser posterior a la inicial; dividir los turnos nocturnos en dos reglas")
    public boolean isHorarioValido() { return horaInicio==null || horaFin==null || horaFin.isAfter(horaInicio); }
}
