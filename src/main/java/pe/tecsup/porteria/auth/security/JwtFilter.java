package pe.tecsup.porteria.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.tecsup.porteria.auth.dto.UsuarioAutenticado;
import pe.tecsup.porteria.auth.service.UsuarioService;
import pe.tecsup.porteria.shared.security.SecurityErrorHandler;

/** Se registra exclusivamente en la cadena de Spring Security, no como filtro servlet independiente. */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final SecurityErrorHandler errors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            rechazar(request, response);
            return;
        }
        try {
            var identidad = jwtService.validar(header.substring(7));
            UsuarioAutenticado usuario = usuarioService.buscarActivo(identidad.id()).orElse(null);
            if (usuario == null || usuario.sesionVersion() != identidad.version()) {
                rechazar(request, response);
                return;
            }
            var authentication = UsernamePasswordAuthenticationToken.authenticated(usuario, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException exception) {
            rechazar(request, response);
            return;
        } catch (DataAccessException | CannotCreateTransactionException exception) {
            log.error("No se pudo consultar el usuario durante la autenticación");
            errors.responder(request, response, HttpStatus.SERVICE_UNAVAILABLE, "Servicio temporalmente no disponible");
            return;
        }
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        errors.responder(request, response, HttpStatus.UNAUTHORIZED, "Credenciales inválidas o sesión expirada");
    }
}
