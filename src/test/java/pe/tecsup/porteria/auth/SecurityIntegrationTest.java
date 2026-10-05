package pe.tecsup.porteria.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.auth.entity.Rol;
import pe.tecsup.porteria.auth.entity.Usuario;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;
import pe.tecsup.porteria.auth.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SecurityIntegrationTest.Endpoints.class})
@Transactional
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;

    @Test
    void sinTokenResponde401ConApiError() throws Exception {
        mvc.perform(get("/api/admin/prueba")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/admin/prueba"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void adminAccedeAlPanelYPorteriaSinCrearSesion() throws Exception {
        String token = token(Rol.ADMIN);
        var respuesta = mvc.perform(get("/api/admin/prueba").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertThat(respuesta.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/portero/prueba").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void porteroNoPuedeAdministrar() throws Exception {
        String token = token(Rol.PORTERO);
        mvc.perform(get("/api/admin/prueba").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/portero/prueba").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void desactivarUsuarioInvalidaSuTokenVigente() throws Exception {
        String token = token(Rol.ADMIN);
        Usuario usuario = usuarios.findByUsername("prueba").orElseThrow();
        usuario.setActivo(false);
        usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/admin/prueba").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambioDeRolSeAplicaAunqueElJwtSigaVigente() throws Exception {
        String token = token(Rol.ADMIN);
        Usuario usuario = usuarios.findByUsername("prueba").orElseThrow();
        usuario.setRol(Rol.PORTERO);
        usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/admin/prueba").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaTokenInvalidoYUsuarioInexistente() throws Exception {
        mvc.perform(get("/api/admin/prueba").header("Authorization", "Bearer incorrecto"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/prueba").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/prueba").header("Authorization", "Bearer " + jwt.generar(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtDelPanelNoAutorizaDispositivos() throws Exception {
        mvc.perform(get("/api/dispositivo/ping").header("Authorization", "Bearer " + token(Rol.ADMIN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bcryptGuardaHashYVerificaContrasena() {
        String hash = encoder.encode("Clave-de-prueba-123");
        assertThat(hash).isNotEqualTo("Clave-de-prueba-123");
        assertThat(encoder.matches("Clave-de-prueba-123", hash)).isTrue();
        assertThat(encoder.matches("incorrecta", hash)).isFalse();
    }

    private String token(Rol rol) {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("prueba", encoder.encode("Clave-de-prueba-123"),
                "Usuario Prueba", rol));
        return jwt.generar(usuario.getId());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Endpoints {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/api/admin/prueba", "/api/portero/prueba"})
        Map<String, Boolean> prueba() { return Map.of("ok", true); }
    }
}
