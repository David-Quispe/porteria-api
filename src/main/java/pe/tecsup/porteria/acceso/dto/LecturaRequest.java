package pe.tecsup.porteria.acceso.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.acceso.entity.*;
@Schema(example = "{\"metodo\":\"NFC\",\"valor\":\"04AA0001\",\"direccion\":\"ENTRADA\"}" )
public record LecturaRequest(@NotNull MetodoId metodo,@NotBlank @Size(max=100) String valor,@NotNull Direccion direccion) {}
