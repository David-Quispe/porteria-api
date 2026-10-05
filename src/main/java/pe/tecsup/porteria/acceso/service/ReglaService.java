package pe.tecsup.porteria.acceso.service;
import java.time.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.entity.*;
import pe.tecsup.porteria.acceso.repository.ReglaAccesoRepository;
import pe.tecsup.porteria.persona.entity.TipoPersona;
import pe.tecsup.porteria.dispositivo.entity.Punto;
import pe.tecsup.porteria.shared.dto.*;
import pe.tecsup.porteria.shared.exception.NotFoundException;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ReglaService {
    private final ReglaAccesoRepository repository;
    public boolean permite(TipoPersona tipo,Punto punto,LocalDateTime fecha) {
        var reglas=repository.aplicables(tipo,punto);
        LocalTime hora=fecha.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        return reglas.isEmpty() || reglas.stream().anyMatch(r -> r.getDias().contains(fecha.getDayOfWeek())
                && !hora.isBefore(r.getHoraInicio()) && !hora.isAfter(r.getHoraFin()));
    }
    public PageResponse<ReglaResponse> listar(int page,int size) {
        return PageResponse.of(repository.findAll(Paginacion.of(page,size,Sort.by("id"))).map(ReglaResponse::of));
    }
    public ReglaResponse obtener(Long id) { return ReglaResponse.of(entidad(id)); }
    @Transactional public ReglaResponse guardar(Long id,ReglaRequest request) {
        var r=id==null ? new ReglaAcceso() : entidad(id);
        r.setTipoPersona(request.tipoPersona()); r.setPunto(request.punto());
        r.setDias(request.dias().stream().map(DayOfWeek::of).collect(Collectors.toSet()));
        LocalTime inicio=request.horaInicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        LocalTime fin=request.horaFin().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        if (!fin.isAfter(inicio)) throw new pe.tecsup.porteria.shared.exception.BusinessException(
                org.springframework.http.HttpStatus.BAD_REQUEST,"El horario debe tener al menos un segundo de duración");
        r.setHoraInicio(inicio); r.setHoraFin(fin); r.setDescripcion(request.descripcion());
        return ReglaResponse.of(repository.saveAndFlush(r));
    }
    @Transactional public void estado(Long id,boolean activa) { entidad(id).setActiva(activa); }
    private ReglaAcceso entidad(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Regla",id)); }
}
