package pe.tecsup.porteria.auth.service;

import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.tecsup.porteria.auth.dto.LoginRequest;
import pe.tecsup.porteria.auth.dto.LoginResponse;
import pe.tecsup.porteria.auth.dto.UsuarioAutenticado;
import pe.tecsup.porteria.auth.entity.Usuario;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;
import pe.tecsup.porteria.auth.security.JwtService;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String hashParaUsuarioDesconocido;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.hashParaUsuarioDesconocido = encoder.encode("usuario-desconocido-sin-acceso");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // BCrypt acepta hasta 72 bytes, no 72 caracteres Unicode. Evita truncamiento o errores 500.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw credencialesInvalidas();
        }
        Usuario usuario = usuarios.findByUsername(request.username().strip()).orElse(null);
        String hash = usuario == null ? hashParaUsuarioDesconocido : usuario.getPasswordHash();
        boolean passwordValida = encoder.matches(request.password(), hash);
        if (usuario == null || !passwordValida || !usuario.isActivo()) {
            throw credencialesInvalidas();
        }
        return new LoginResponse(jwt.generar(usuario.getId(), usuario.getSesionVersion()), "Bearer", jwt.duracionSegundos(),
                UsuarioAutenticado.of(usuario));
    }

    private BadCredentialsException credencialesInvalidas() {
        return new BadCredentialsException("Credenciales inválidas o sesión expirada");
    }
}
