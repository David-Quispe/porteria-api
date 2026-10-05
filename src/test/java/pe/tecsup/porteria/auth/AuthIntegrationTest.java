package pe.tecsup.porteria.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;

    @Test
    void adminInicialIniciaSesionYConsultaSuIdentidad() throws Exception {
        var respuesta = login("admin.dev", "PorteriaDev-2026!").andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(28_800))
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist()).andReturn();
        String token = json.readTree(respuesta.getResponse().getContentAsString()).get("accessToken").asString();
        assertThat(token).isNotBlank();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin.dev"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void contrasenaIncorrectaYUsuarioDesconocidoDanElMismoError() throws Exception {
        login("admin.dev", "incorrecta").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas o sesión expirada"));
        login("no-existe", "incorrecta").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas o sesión expirada"));
    }

    @Test
    void usuarioDesactivadoNoPuedeIniciarSesion() throws Exception {
        var usuario = usuarios.findByUsername("admin.dev").orElseThrow();
        usuario.setActivo(false);
        usuarios.saveAndFlush(usuario);
        login("admin.dev", "PorteriaDev-2026!").andExpect(status().isUnauthorized());
    }

    @Test
    void validaCamposYRechazaJsonMalformado() throws Exception {
        login("", "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        login("admin.dev", "ñ".repeat(40)).andExpect(status().isUnauthorized());
    }

    @Test
    void meSinTokenNoExponeIdentidad() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))));
    }
}
