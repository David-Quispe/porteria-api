package pe.tecsup.porteria.persona.service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tecsup.porteria.persona.dto.*;
import pe.tecsup.porteria.persona.entity.*;
import pe.tecsup.porteria.persona.repository.*;
import pe.tecsup.porteria.shared.exception.NotFoundException;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CredencialAdminService {
    private final CredencialRepository repository;
    private final PersonaRepository personas;
    public List<CredencialResponse> listar(Long personaId) {
        persona(personaId);
        return repository.findByPersonaIdOrderById(personaId).stream().map(CredencialResponse::of).toList();
    }
    @Transactional
    public CredencialResponse crear(Long personaId, CredencialRequest request) {
        var credencial = new Credencial(persona(personaId), request.tipo(),
                ValorCredencial.normalizar(request.tipo(), request.valor()));
        return CredencialResponse.of(repository.saveAndFlush(credencial));
    }
    @Transactional
    public void estado(Long personaId, Long id, boolean activa) {
        Credencial c = obtener(personaId, id);
        c.setActiva(activa);
        repository.flush();
    }
    @Transactional
    public void eliminar(Long personaId, Long id) { repository.delete(obtener(personaId, id)); }
    private Persona persona(Long id) {
        return personas.findById(id).orElseThrow(() -> new NotFoundException("Persona", id));
    }
    private Credencial obtener(Long personaId, Long id) {
        return repository.findByIdAndPersonaId(id, personaId)
                .orElseThrow(() -> new NotFoundException("Credencial", id));
    }
}
