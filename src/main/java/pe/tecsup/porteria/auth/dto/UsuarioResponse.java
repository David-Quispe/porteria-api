package pe.tecsup.porteria.auth.dto;
import pe.tecsup.porteria.auth.entity.*;
public record UsuarioResponse(Long id,String username,String nombre,Rol rol,boolean activo) {
    public static UsuarioResponse of(Usuario u) { return new UsuarioResponse(u.getId(),u.getUsername(),u.getNombre(),u.getRol(),u.isActivo()); }
}
