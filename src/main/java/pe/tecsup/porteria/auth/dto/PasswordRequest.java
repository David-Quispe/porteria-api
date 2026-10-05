package pe.tecsup.porteria.auth.dto;
import jakarta.validation.constraints.*;
public record PasswordRequest(@NotBlank @Size(min=12,max=72) String password) {}
