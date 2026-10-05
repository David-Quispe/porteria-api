package pe.tecsup.porteria.acceso.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.tecsup.porteria.acceso.entity.MetodoId;
import pe.tecsup.porteria.acceso.entity.RegistroAcceso;
import pe.tecsup.porteria.acceso.entity.Resultado;

/**
 * Escribir registros es exclusivo de AccesoService. El módulo reporte puede usar este repositorio
 * solo para consultas con Specifications (guía, regla 4.3).
 */
public interface RegistroAccesoRepository
        extends JpaRepository<RegistroAcceso, Long>, JpaSpecificationExecutor<RegistroAcceso> {

    Optional<RegistroAcceso> findFirstByOrderByFechaHoraDescIdDesc();

    Optional<RegistroAcceso> findFirstByPersonaIdAndResultadoOrderByFechaHoraDescIdDesc(Long personaId, Resultado resultado);

    Optional<RegistroAcceso> findFirstByDispositivoIdAndMetodoAndValorLeidoAndDireccionAndFechaHoraGreaterThanOrderByFechaHoraDesc(
            Long dispositivoId, MetodoId metodo, String valorLeido, pe.tecsup.porteria.acceso.entity.Direccion direccion, LocalDateTime desde);

    /**
     * Filtro de lectura duplicada: la última lectura del mismo valor en el mismo dispositivo desde {@code desde}.
     * Usa el índice ix_registro_duplicado.
     */
    Optional<RegistroAcceso> findFirstByDispositivoIdAndMetodoAndValorLeidoAndFechaHoraGreaterThanEqualOrderByFechaHoraDesc(
            Long dispositivoId, MetodoId metodo, String valorLeido, LocalDateTime desde);
}
