package pe.tecsup.porteria.acceso.dto;
import jakarta.validation.constraints.*;
import pe.tecsup.porteria.acceso.entity.*;
public record LecturaRequest(@NotNull MetodoId metodo,@NotBlank @Size(max=100) String valor,@NotNull Direccion direccion) {}
