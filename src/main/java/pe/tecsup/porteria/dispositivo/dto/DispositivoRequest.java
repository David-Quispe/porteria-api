package pe.tecsup.porteria.dispositivo.dto;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.dispositivo.entity.Punto;
public record DispositivoRequest(@NotBlank @Size(max=100) String nombre, @NotNull Punto punto) {}
