package pe.tecsup.porteria.acceso.event;
import java.time.LocalDateTime;
import pe.tecsup.porteria.acceso.entity.*;
public record AccesoRegistradoEvent(Long id,LocalDateTime fechaHora,Long dispositivoId,Long personaId,
        String nombre,String fotoUrl,MetodoId metodo,Direccion direccion,Resultado resultado) {}
