package pe.tecsup.porteria.persona.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.persona.entity.Persona;
import pe.tecsup.porteria.persona.repository.PersonaRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonaService {

    private final PersonaRepository personaRepository;

    /** Incluye personas inactivas o vencidas: acceso debe distinguirlas de un DNI desconocido. */
    public Optional<Persona> buscarPorDni(String dni) {
        if (dni == null || dni.isBlank()) {
            return Optional.empty();
        }
        return personaRepository.findByDni(dni.strip());
    }

    /** Consulta en lote para que los reportes no busquen una persona por cada registro. */
    public List<Persona> buscarPorIds(Set<Long> ids) {
        return ids.isEmpty() ? List.of() : personaRepository.findAllById(ids);
    }
}
