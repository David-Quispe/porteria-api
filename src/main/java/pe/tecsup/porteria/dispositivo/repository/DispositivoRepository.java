package pe.tecsup.porteria.dispositivo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.tecsup.porteria.dispositivo.entity.Dispositivo;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from Dispositivo d where d.id = :id")
    Optional<Dispositivo> bloquear(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Dispositivo> findByTokenHashAndActivoTrue(String tokenHash);
}
