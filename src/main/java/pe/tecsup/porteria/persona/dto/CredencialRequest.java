package pe.tecsup.porteria.persona.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.persona.entity.TipoCredencial;
@Schema(example = "{\"tipo\":\"NFC\",\"valor\":\"04AA0001\"}" )
public record CredencialRequest(@NotNull TipoCredencial tipo, @NotBlank @Size(max=100) String valor) {}
