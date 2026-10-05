package pe.tecsup.porteria.persona.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.persona.dto.*;
import pe.tecsup.porteria.persona.entity.*;
import pe.tecsup.porteria.persona.mapper.PersonaMapper;
import pe.tecsup.porteria.persona.repository.PersonaRepository;
import pe.tecsup.porteria.shared.dto.*;
import pe.tecsup.porteria.shared.exception.NotFoundException;
import java.util.Locale;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonaAdminService {
    private final PersonaRepository repository;
    private final PersonaMapper mapper;

    public PageResponse<PersonaResponse> listar(String q, TipoPersona tipo, int page, int size) {
        Specification<Persona> spec = (r, query, cb) -> cb.conjunction();
        if (tipo != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("tipo"), tipo));
        if (q != null && !q.isBlank()) {
            String term = "%" + q.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, query, cb) -> cb.or(
                    cb.like(cb.lower(r.get("nombres")), term, '\\'),
                    cb.like(cb.lower(r.get("apellidos")), term, '\\'), cb.like(r.get("dni"), term, '\\')));
        }
        return PageResponse.of(repository.findAll(spec, Paginacion.of(page, size, Sort.by("id"))).map(mapper::toResponse));
    }

    public PersonaResponse obtener(Long id) { return mapper.toResponse(entidad(id)); }

    @Transactional
    public PersonaResponse guardar(Long id, PersonaRequest request) {
        Persona persona = id == null ? new Persona() : entidad(id);
        mapper.update(request, persona);
        persona.setNombres(persona.getNombres().strip());
        persona.setApellidos(persona.getApellidos().strip());
        persona.setCodigo(request.codigo() == null || request.codigo().isBlank() ? null : request.codigo().strip());
        return mapper.toResponse(repository.saveAndFlush(persona));
    }

    @Transactional
    public void estado(Long id, boolean activo) { entidad(id).setActivo(activo); }

    private Persona entidad(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Persona", id));
    }
}
