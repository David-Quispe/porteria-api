package pe.tecsup.porteria.dispositivo.security;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.tecsup.porteria.dispositivo.service.DispositivoTokenService;
import pe.tecsup.porteria.shared.security.SecurityErrorHandler;
@RequiredArgsConstructor
public class DeviceTokenFilter extends OncePerRequestFilter {
    private final DispositivoTokenService tokens;
    private final SecurityErrorHandler errors;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String token=request.getHeader("X-Device-Token");
        try {
            var dispositivo=token != null && token.length() <= 128 ? tokens.buscarActivoPorToken(token) : java.util.Optional.<pe.tecsup.porteria.dispositivo.entity.Dispositivo>empty();
            if (dispositivo.isEmpty()) {
                errors.responder(request,response,HttpStatus.UNAUTHORIZED,"Token de dispositivo inválido"); return;
            }
            var d=dispositivo.get();
            var context=SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    new DispositivoAutenticado(d.getId(),d.getTokenHash()),null,List.of(new SimpleGrantedAuthority("DEVICE"))));
            SecurityContextHolder.setContext(context);
        } catch (DataAccessException | CannotCreateTransactionException ex) {
            errors.responder(request,response,HttpStatus.SERVICE_UNAVAILABLE,"Servicio temporalmente no disponible"); return;
        }
        chain.doFilter(request,response);
    }
}
