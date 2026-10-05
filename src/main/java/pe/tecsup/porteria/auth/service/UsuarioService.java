package pe.tecsup.porteria.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.auth.dto.UsuarioAutenticado;
import pe.tecsup.porteria.auth.entity.Usuario;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public Optional<UsuarioAutenticado> buscarActivo(Long id) {
        return usuarioRepository.findById(id).filter(Usuario::isActivo).map(UsuarioAutenticado::of);
    }
}
