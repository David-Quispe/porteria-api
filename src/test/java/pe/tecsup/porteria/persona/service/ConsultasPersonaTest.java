package pe.tecsup.porteria.persona.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.tecsup.porteria.persona.entity.TipoCredencial;
import pe.tecsup.porteria.persona.repository.CredencialRepository;
import pe.tecsup.porteria.persona.repository.PersonaRepository;

@ExtendWith(MockitoExtension.class)
class ConsultasPersonaTest {

    @Mock PersonaRepository personas;
    @Mock CredencialRepository credenciales;

    @Test
    void consultasVaciasNoAccedenALaBase() {
        PersonaService personaService = new PersonaService(personas);
        CredencialService credencialService = new CredencialService(credenciales);

        assertThat(personaService.buscarPorDni(null)).isEmpty();
        assertThat(personaService.buscarPorDni("  ")).isEmpty();
        assertThat(personaService.buscarPorIds(Set.of())).isEmpty();
        assertThat(credencialService.buscarActiva(null, "abc")).isEmpty();
        assertThat(credencialService.buscarActiva(TipoCredencial.NFC, null)).isEmpty();
        assertThat(credencialService.buscarActiva(TipoCredencial.QR, " ")).isEmpty();
        verifyNoInteractions(personas, credenciales);
    }
}
