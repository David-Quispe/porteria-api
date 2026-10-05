package pe.tecsup.porteria.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.tecsup.porteria.auth.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    long countByRolAndActivoTrue(pe.tecsup.porteria.auth.entity.Rol rol);

    Optional<Usuario> findByUsername(String username);
}
