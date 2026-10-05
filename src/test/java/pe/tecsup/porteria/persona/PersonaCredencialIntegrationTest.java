package pe.tecsup.porteria.persona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.persona.entity.Credencial;
import pe.tecsup.porteria.persona.entity.Persona;
import pe.tecsup.porteria.persona.entity.TipoCredencial;
import pe.tecsup.porteria.persona.entity.TipoPersona;
import pe.tecsup.porteria.persona.repository.CredencialRepository;
import pe.tecsup.porteria.persona.repository.PersonaRepository;
import pe.tecsup.porteria.persona.service.CredencialService;
import pe.tecsup.porteria.persona.service.PersonaService;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PersonaCredencialIntegrationTest {

    @Autowired PersonaRepository personas;
    @Autowired CredencialRepository credenciales;
    @Autowired PersonaService personaService;
    @Autowired CredencialService credencialService;
    @Autowired EntityManager entityManager;

    @Test
    void encuentraPorDniAunqueEsteInactivaYVencida() {
        Persona persona = nuevaPersona("00123456");
        persona.setActivo(false);
        persona.setVigenciaInicio(LocalDate.of(2020, 1, 1));
        persona.setVigenciaFin(LocalDate.of(2020, 12, 31));
        personas.saveAndFlush(persona);
        entityManager.clear();

        Persona encontrada = personaService.buscarPorDni(" 00123456 ").orElseThrow();
        assertThat(encontrada.isActivo()).isFalse();
        assertThat(encontrada.getVigenciaFin()).isEqualTo(LocalDate.of(2020, 12, 31));
        assertThat(encontrada.getCreadoEn()).isNotNull();
        assertThat(encontrada.getActualizadoEn()).isEqualTo(encontrada.getCreadoEn());
        assertThat(personaService.buscarPorDni("99999999")).isEmpty();
    }

    @Test
    void credencialIncluyePersonaFueraDelContextoDePersistencia() {
        Persona persona = personas.save(nuevaPersona("00123456"));
        persona.setActivo(false);
        credenciales.saveAndFlush(new Credencial(persona, TipoCredencial.NFC, "04ABCDEF"));
        entityManager.clear();

        Credencial encontrada = credencialService.buscarActiva(TipoCredencial.NFC, "04ABCDEF").orElseThrow();
        entityManager.clear();
        assertThat(encontrada.getPersona().getNombres()).isEqualTo("Ana");
        assertThat(encontrada.getPersona().isActivo()).isFalse();
        assertThat(encontrada.getCreadoEn()).isNotNull();
    }

    @Test
    void permiteReasignarUnaCredencialDesactivada() {
        Persona anterior = personas.save(nuevaPersona("00123456"));
        Persona nueva = personas.save(nuevaPersona("00234567"));
        Credencial desactivada = new Credencial(anterior, TipoCredencial.NFC, "04ABCDEF");
        desactivada.setActiva(false);
        credenciales.saveAndFlush(desactivada);
        assertThat(credencialService.buscarActiva(TipoCredencial.NFC, "04ABCDEF")).isEmpty();

        credenciales.saveAndFlush(new Credencial(nueva, TipoCredencial.NFC, "04ABCDEF"));
        entityManager.clear();
        assertThat(credencialService.buscarActiva(TipoCredencial.NFC, "04ABCDEF").orElseThrow()
                .getPersona().getId()).isEqualTo(nueva.getId());
    }

    @Test
    void rechazaElMismoValorActivoEnDosPersonas() {
        Persona primera = personas.save(nuevaPersona("00123456"));
        Persona segunda = personas.save(nuevaPersona("00234567"));
        credenciales.saveAndFlush(new Credencial(primera, TipoCredencial.QR, "qr-personal"));
        assertThatThrownBy(() -> credenciales.saveAndFlush(
                new Credencial(segunda, TipoCredencial.QR, "qr-personal")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void distingueTipoYConservaElValorExactoDelQr() {
        Persona persona = personas.save(nuevaPersona("00123456"));
        credenciales.saveAndFlush(new Credencial(persona, TipoCredencial.QR, "AbC123"));
        assertThat(credencialService.buscarActiva(TipoCredencial.QR, "AbC123")).isPresent();
        assertThat(credencialService.buscarActiva(TipoCredencial.NFC, "AbC123")).isEmpty();
        assertThat(credencialService.buscarActiva(TipoCredencial.QR, "abc123")).isEmpty();
    }

    @Test
    void rechazaDniDuplicado() {
        personas.saveAndFlush(nuevaPersona("00123456"));
        assertThatThrownBy(() -> personas.saveAndFlush(nuevaPersona("00123456")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaVigenciaInvertida() {
        Persona persona = nuevaPersona("00123456");
        persona.setVigenciaInicio(LocalDate.of(2026, 2, 1));
        persona.setVigenciaFin(LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> personas.saveAndFlush(persona))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void consultaPersonasEnLoteSinIncluirOtras() {
        Persona primera = personas.save(nuevaPersona("00123456"));
        Persona segunda = personas.save(nuevaPersona("00234567"));
        personas.saveAndFlush(nuevaPersona("00345678"));
        assertThat(personaService.buscarPorIds(Set.of(primera.getId(), segunda.getId())))
                .extracting(Persona::getDni).containsExactlyInAnyOrder("00123456", "00234567");
    }

    private Persona nuevaPersona(String dni) {
        return new Persona(TipoPersona.VISITANTE, dni, "Ana", "Quispe");
    }
}
