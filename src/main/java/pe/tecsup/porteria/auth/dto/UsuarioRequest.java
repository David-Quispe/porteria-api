package pe.tecsup.porteria.auth.dto;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.auth.entity.Rol;
public record UsuarioRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,50}") String username,
        @NotBlank @Size(min=12,max=72) String password,@NotBlank @Size(max=120) String nombre,@NotNull Rol rol) {}
