package pe.tecsup.porteria.acceso.websocket;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.auth.dto.UsuarioAutenticado;
import pe.tecsup.porteria.auth.security.JwtService;
import pe.tecsup.porteria.auth.service.UsuarioService;

/** Cada sesión STOMP presenta un JWT; se revalida al suscribirse para respetar revocaciones. */
@Component
@RequiredArgsConstructor
public class AccesoWebSocketAuth implements ChannelInterceptor {
    private final JwtService jwt;
    private final UsuarioService usuarios;
    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) return message;
        String session = accessor.getSessionId();
        if (command == StompCommand.MESSAGE) {
            if (session == null) return null;
            try {
                autenticar(tokens.get(session));
                return message;
            } catch (SecurityException exception) {
                return null;
            }
        }
        if (command == StompCommand.DISCONNECT) {
            if (session != null) tokens.remove(session);
            return message;
        }
        if (command == StompCommand.CONNECT) {
            String bearer = accessor.getFirstNativeHeader("Authorization");
            if (bearer == null || !bearer.startsWith("Bearer ") || session == null) {
                throw new SecurityException("Se requiere token Bearer para WebSocket");
            }
            String token = bearer.substring(7);
            UsuarioAutenticado usuario = autenticar(token);
            tokens.put(session, token);
            accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(usuario, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name()))));
            return message;
        }
        if (command == StompCommand.SUBSCRIBE) {
            if (!"/topic/accesos".equals(accessor.getDestination()) || session == null) {
                throw new SecurityException("Suscripción no permitida");
            }
            autenticar(tokens.get(session));
            return message;
        }
        if (command == StompCommand.SEND) throw new SecurityException("Publicación no permitida");
        return message;
    }

    @EventListener
    public void alDesconectar(SessionDisconnectEvent event) {
        tokens.remove(event.getSessionId());
    }

    private UsuarioAutenticado autenticar(String token) {
        if (token == null) throw new SecurityException("Sesión WebSocket inválida");
        try {
            var identidad = jwt.validar(token);
            UsuarioAutenticado usuario = usuarios.buscarActivo(identidad.id())
                    .orElseThrow(() -> new SecurityException("Usuario inactivo"));
            if (usuario.sesionVersion() != identidad.version()) throw new SecurityException("Sesión revocada");
            return usuario;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new SecurityException("Token WebSocket inválido");
        }
    }
}
