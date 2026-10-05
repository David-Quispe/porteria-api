package pe.tecsup.porteria.auth.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.auth.entity.Rol;
@Schema(example = "{\"username\":\"portero.1\",\"password\":\"ClaveSegura-2026!\",\"nombre\":\"Portero principal\",\"rol\":\"PORTERO\"}" )
public record UsuarioRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,50}") String username,
        @NotBlank @Size(min=12,max=72) String password,@NotBlank @Size(max=120) String nombre,@NotNull Rol rol) {}
