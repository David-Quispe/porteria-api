package pe.tecsup.porteria.auth.dto;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.auth.entity.Rol;
public record UsuarioUpdate(@NotBlank @Size(max=120) String nombre,@NotNull Rol rol) {}
