package pe.tecsup.porteria.persona.dto;
import java.time.LocalDate;
import pe.tecsup.porteria.persona.entity.TipoPersona;
public record PersonaResponse(Long id, TipoPersona tipo, String dni, String nombres, String apellidos,
        String codigo, String area, String fotoUrl, LocalDate vigenciaInicio, LocalDate vigenciaFin, boolean activo) {}
