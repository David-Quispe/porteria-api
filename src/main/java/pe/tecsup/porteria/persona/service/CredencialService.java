package pe.tecsup.porteria.persona.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.persona.entity.Credencial;
import pe.tecsup.porteria.persona.entity.TipoCredencial;
import pe.tecsup.porteria.persona.repository.CredencialRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CredencialService {

    private final CredencialRepository credencialRepository;

    /** Recibe el valor ya normalizado por el flujo de lectura o de administración. */
    public Optional<Credencial> buscarActiva(TipoCredencial tipo, String valor) {
        if (tipo == null || valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        return credencialRepository.findByTipoAndValorAndActivaTrue(tipo, valor);
    }
}
