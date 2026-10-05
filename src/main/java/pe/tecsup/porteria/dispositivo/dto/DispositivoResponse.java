package pe.tecsup.porteria.dispositivo.dto;
import java.time.LocalDateTime;
import pe.tecsup.porteria.dispositivo.entity.*;
public record DispositivoResponse(Long id, String nombre, Punto punto, boolean activo, LocalDateTime ultimoPing) {
    public static DispositivoResponse of(Dispositivo d) {
        return new DispositivoResponse(d.getId(),d.getNombre(),d.getPunto(),d.isActivo(),d.getUltimoPing());
    }
}
