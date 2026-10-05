package pe.tecsup.porteria.persona.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.tecsup.porteria.persona.entity.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Long>, JpaSpecificationExecutor<Persona> {

    Optional<Persona> findByDni(String dni);
}
