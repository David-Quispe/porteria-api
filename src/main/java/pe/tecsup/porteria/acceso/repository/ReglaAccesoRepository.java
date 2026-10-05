package pe.tecsup.porteria.acceso.repository;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.tecsup.porteria.acceso.entity.ReglaAcceso;
import pe.tecsup.porteria.persona.entity.TipoPersona;
import pe.tecsup.porteria.dispositivo.entity.Punto;
public interface ReglaAccesoRepository extends JpaRepository<ReglaAcceso,Long> {
    @Query("select r from ReglaAcceso r where r.activa=true and r.tipoPersona=:tipo and (r.punto is null or r.punto=:punto)")
    List<ReglaAcceso> aplicables(@Param("tipo") TipoPersona tipo,@Param("punto") Punto punto);
}
