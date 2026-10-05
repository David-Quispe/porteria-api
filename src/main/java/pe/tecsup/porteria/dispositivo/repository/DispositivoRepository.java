package pe.tecsup.porteria.dispositivo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.tecsup.porteria.dispositivo.entity.Dispositivo;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    Optional<Dispositivo> findByTokenHashAndActivoTrue(String tokenHash);
}
