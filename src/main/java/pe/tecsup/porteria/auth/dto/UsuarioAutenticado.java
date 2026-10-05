package pe.tecsup.porteria.auth.dto;

import pe.tecsup.porteria.auth.entity.Rol;
import pe.tecsup.porteria.auth.entity.Usuario;

/** Identidad de la sesión, sin contraseña ni entidad JPA en el contexto de seguridad. */
public record UsuarioAutenticado(Long id, String username, String nombre, Rol rol,
        @com.fasterxml.jackson.annotation.JsonIgnore long sesionVersion) {

    public static UsuarioAutenticado of(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getUsername(), usuario.getNombre(), usuario.getRol(), usuario.getSesionVersion());
    }
}
