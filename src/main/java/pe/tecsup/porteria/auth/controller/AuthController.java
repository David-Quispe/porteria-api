package pe.tecsup.porteria.auth.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.auth.dto.LoginRequest;
import pe.tecsup.porteria.auth.dto.LoginResponse;
import pe.tecsup.porteria.auth.dto.UsuarioAutenticado;
import pe.tecsup.porteria.auth.service.AuthService;
import pe.tecsup.porteria.shared.config.OpenApiConfig;
import pe.tecsup.porteria.shared.security.RateLimitService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión y usuario autenticado del panel")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimit;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Devuelve un JWT Bearer. expiresIn se expresa en segundos.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        rateLimit.login(servletRequest.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar el usuario autenticado")
    @SecurityRequirement(name = OpenApiConfig.JWT)
    public ResponseEntity<UsuarioAutenticado> me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usuario);
    }
}
