package pe.tecsup.porteria.persona.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.tecsup.porteria.persona.entity.Credencial;
import pe.tecsup.porteria.persona.entity.TipoCredencial;

public interface CredencialRepository extends JpaRepository<Credencial, Long> {

    java.util.List<Credencial> findByPersonaIdOrderById(Long personaId);
    Optional<Credencial> findByIdAndPersonaId(Long id, Long personaId);

    // La persona queda disponible al salir de la transacción, con open-in-view desactivado.
    @EntityGraph(attributePaths = "persona")
    Optional<Credencial> findByTipoAndValorAndActivaTrue(TipoCredencial tipo, String valor);
}
