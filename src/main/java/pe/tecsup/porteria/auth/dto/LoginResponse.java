package pe.tecsup.porteria.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioAutenticado usuario) {
}
