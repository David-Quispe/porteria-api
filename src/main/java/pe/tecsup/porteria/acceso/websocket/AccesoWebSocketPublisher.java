package pe.tecsup.porteria.acceso.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.acceso.event.AccesoRegistradoEvent;

@Component
@RequiredArgsConstructor
public class AccesoWebSocketPublisher {
    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(AccesoRegistradoEvent event) {
        messaging.convertAndSend("/topic/accesos", event);
    }
}
