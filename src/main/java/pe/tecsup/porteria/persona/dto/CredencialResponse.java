package pe.tecsup.porteria.persona.dto;
import pe.tecsup.porteria.persona.entity.*;
public record CredencialResponse(Long id, TipoCredencial tipo, String valor, boolean activa) {
    public static CredencialResponse of(Credencial c) {
        return new CredencialResponse(c.getId(), c.getTipo(), c.getValor(), c.isActiva());
    }
}
