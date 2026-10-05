package pe.tecsup.porteria.acceso.websocket;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import pe.tecsup.porteria.auth.dto.UsuarioAutenticado;
import pe.tecsup.porteria.auth.entity.Rol;
import pe.tecsup.porteria.auth.security.JwtService;
import pe.tecsup.porteria.auth.service.UsuarioService;

class AccesoWebSocketAuthTest {
    private final JwtService jwt = new JwtService("c2VjcmV0by1kZS1wcnVlYmEtc29sby1wYXJhLXByb2ZpbGUtcHJvZA==", Duration.ofHours(1));
    private final UsuariosPrueba usuarios = new UsuariosPrueba();
    private final MessageChannel channel = (message, timeout) -> true;
    private final AccesoWebSocketAuth auth = new AccesoWebSocketAuth(jwt, usuarios);

    @Test
    void exigeTokenPermiteSoloTopicYRevocaAlCambiarVersion() {
        String token = jwt.generar(7L, 2);

        assertThrows(SecurityException.class, () -> auth.preSend(mensaje(StompCommand.CONNECT, null, null), channel));
        assertNotNull(auth.preSend(mensaje(StompCommand.CONNECT, null, "Bearer " + token), channel));
        assertNotNull(auth.preSend(mensaje(StompCommand.SUBSCRIBE, "/topic/accesos", null), channel));
        assertThrows(SecurityException.class, () -> auth.preSend(mensaje(StompCommand.SUBSCRIBE, "/topic/otro", null), channel));
        assertThrows(SecurityException.class, () -> auth.preSend(mensaje(StompCommand.SEND, "/topic/accesos", null), channel));
        assertNotNull(auth.preSend(mensaje(StompCommand.MESSAGE, "/topic/accesos", null), channel));

        usuarios.version = 3;
        assertThrows(SecurityException.class, () -> auth.preSend(mensaje(StompCommand.SUBSCRIBE, "/topic/accesos", null), channel));
        assertNull(auth.preSend(mensaje(StompCommand.MESSAGE, "/topic/accesos", null), channel));
    }

    private Message<byte[]> mensaje(StompCommand command, String destino, String bearer) {
        var headers = StompHeaderAccessor.create(command);
        headers.setSessionId("sesion-1");
        if (destino != null) headers.setDestination(destino);
        if (bearer != null) headers.setNativeHeader("Authorization", bearer);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private static final class UsuariosPrueba extends UsuarioService {
        private long version = 2;
        private UsuariosPrueba() { super(null); }
        @Override public Optional<UsuarioAutenticado> buscarActivo(Long id) {
            return Optional.of(new UsuarioAutenticado(id, "portero", "Portero", Rol.PORTERO, version));
        }
    }
}
