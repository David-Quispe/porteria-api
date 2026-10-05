package pe.tecsup.porteria.acceso.dto;
import java.time.*;
import java.util.Set;
import java.util.stream.Collectors;
import pe.tecsup.porteria.persona.entity.TipoPersona;
import pe.tecsup.porteria.dispositivo.entity.Punto;
import pe.tecsup.porteria.acceso.entity.ReglaAcceso;
public record ReglaResponse(Long id,TipoPersona tipoPersona,Punto punto,Set<Integer> dias,
        LocalTime horaInicio,LocalTime horaFin,String descripcion,boolean activa) {
    public static ReglaResponse of(ReglaAcceso r) { return new ReglaResponse(r.getId(),r.getTipoPersona(),r.getPunto(),
            r.getDias().stream().map(DayOfWeek::getValue).collect(Collectors.toSet()),r.getHoraInicio(),r.getHoraFin(),r.getDescripcion(),r.isActiva()); }
}
