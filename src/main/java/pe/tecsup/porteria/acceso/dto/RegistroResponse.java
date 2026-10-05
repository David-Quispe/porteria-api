package pe.tecsup.porteria.acceso.dto;
import java.time.LocalDateTime;
import pe.tecsup.porteria.acceso.entity.*;
public record RegistroResponse(Long id,LocalDateTime fechaHora,Long dispositivoId,Long personaId,
        String nombre,String fotoUrl,MetodoId metodo,Direccion direccion,Resultado resultado) {}
