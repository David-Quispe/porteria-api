package pe.tecsup.porteria.acceso.dto;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import pe.tecsup.porteria.acceso.entity.*;
public record RegistroFiltro(
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
        Long personaId,Long dispositivoId,MetodoId metodo,Resultado resultado,Direccion direccion) {}
