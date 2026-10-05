package pe.tecsup.porteria.auth;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;
import pe.tecsup.porteria.auth.security.JwtService;
import tools.jackson.databind.ObjectMapper;
@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class) @Transactional
class UsuarioAdminTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UsuarioRepository usuarios; @Autowired JwtService jwt;
    @Test void cambioDePasswordRevocaTokensAnteriores() throws Exception {
        var admin=usuarios.findByUsername("admin.dev").orElseThrow();String bearer="Bearer "+jwt.generar(admin.getId());
        var result=mvc.perform(post("/api/admin/usuarios").header("Authorization",bearer).contentType("application/json")
                .content("{\"username\":\"portero.test\",\"password\":\"Password-original!\",\"nombre\":\"Portero\",\"rol\":\"PORTERO\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        String viejo="Bearer "+jwt.generar(id);
        mvc.perform(get("/api/auth/me").header("Authorization",viejo)).andExpect(status().isOk());
        mvc.perform(put("/api/admin/usuarios/"+id+"/password").header("Authorization",bearer).contentType("application/json")
                .content("{\"password\":\"Password-nuevo-2026!\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization",viejo)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"portero.test\",\"password\":\"Password-nuevo-2026!\"}")).andExpect(status().isOk());
    }
    @Test void noPermiteDesactivarAlUltimoAdministrador() throws Exception {
        var admin=usuarios.findByUsername("admin.dev").orElseThrow();
        mvc.perform(delete("/api/admin/usuarios/"+admin.getId()).header("Authorization","Bearer "+jwt.generar(admin.getId())))
                .andExpect(status().isConflict());
    }
}
