package pe.tecsup.porteria.dispositivo.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.dispositivo.entity.Punto;
@Schema(example = "{\"nombre\":\"Puerta peatonal\",\"punto\":\"PEATONAL\"}" )
public record DispositivoRequest(@NotBlank @Size(max=100) String nombre, @NotNull Punto punto) {}
