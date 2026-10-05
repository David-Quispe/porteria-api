package pe.tecsup.porteria.acceso.service;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.entity.RegistroAcceso;
import pe.tecsup.porteria.acceso.repository.RegistroAccesoRepository;
import pe.tecsup.porteria.persona.entity.Persona;
import pe.tecsup.porteria.persona.service.PersonaService;
import pe.tecsup.porteria.shared.exception.BusinessException;
@Service @Transactional(readOnly=true)
public class RegistroConsultaService {
    private final RegistroAccesoRepository repository;
    private final PersonaService personas;
    private final Clock clock;
    private final List<LocalTime> turnos;
    public RegistroConsultaService(RegistroAccesoRepository repository,PersonaService personas,Clock clock,
            @Value("${porteria.turnos:06:00,14:00,22:00}") String turnos) {
        this.repository=repository;this.personas=personas;this.clock=clock;
        this.turnos=Arrays.stream(turnos.split(",")).map(String::strip).map(LocalTime::parse).distinct().sorted().toList();
        if(this.turnos.isEmpty()) throw new IllegalArgumentException("Se requiere al menos un turno");
    }
    public Page<RegistroResponse> listar(RegistroFiltro f,Pageable pageable) {
        validar(f);
        Specification<RegistroAcceso> s=(r,q,cb)->cb.conjunction();
        if(f.desde()!=null) s=s.and((r,q,cb)->cb.greaterThanOrEqualTo(r.get("fechaHora"),f.desde()));
        if(f.hasta()!=null) s=s.and((r,q,cb)->cb.lessThanOrEqualTo(r.get("fechaHora"),f.hasta()));
        if(f.personaId()!=null) s=s.and((r,q,cb)->cb.equal(r.get("personaId"),f.personaId()));
        if(f.dispositivoId()!=null) s=s.and((r,q,cb)->cb.equal(r.get("dispositivoId"),f.dispositivoId()));
        if(f.metodo()!=null) s=s.and((r,q,cb)->cb.equal(r.get("metodo"),f.metodo()));
        if(f.resultado()!=null) s=s.and((r,q,cb)->cb.equal(r.get("resultado"),f.resultado()));
        if(f.direccion()!=null) s=s.and((r,q,cb)->cb.equal(r.get("direccion"),f.direccion()));
        var page=repository.findAll(s,pageable);
        var ids=page.stream().map(RegistroAcceso::getPersonaId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long,Persona> nombres=personas.buscarPorIds(ids).stream().collect(Collectors.toMap(Persona::getId,p->p));
        return page.map(r->convertir(r,nombres.get(r.getPersonaId())));
    }
    public Optional<RegistroResponse> ultima() {
        return repository.findFirstByOrderByFechaHoraDescIdDesc().map(r-> {
            Persona p=r.getPersonaId()==null?null:personas.buscarPorIds(Set.of(r.getPersonaId())).stream().findFirst().orElse(null);
            return convertir(r,p);
        });
    }
    public Page<RegistroResponse> turno(Pageable pageable) {
        LocalDateTime ahora=LocalDateTime.now(clock);
        LocalTime inicio=turnos.stream().filter(t->!t.isAfter(ahora.toLocalTime())).reduce((a,b)->b).orElse(null);
        LocalDateTime desde=inicio==null?ahora.toLocalDate().minusDays(1).atTime(turnos.getLast()):ahora.toLocalDate().atTime(inicio);
        return listar(new RegistroFiltro(desde,ahora,null,null,null,null,null),pageable);
    }
    public static void validar(RegistroFiltro f) {
        if(f.desde()!=null && f.hasta()!=null && f.hasta().isBefore(f.desde()))
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,"El rango de fechas está invertido");
    }
    private RegistroResponse convertir(RegistroAcceso r,Persona p) {
        return new RegistroResponse(r.getId(),r.getFechaHora(),r.getDispositivoId(),r.getPersonaId(),
                p==null?null:p.getNombres()+" "+p.getApellidos(),p==null?null:p.getFotoUrl(),r.getMetodo(),r.getDireccion(),r.getResultado());
    }
}
