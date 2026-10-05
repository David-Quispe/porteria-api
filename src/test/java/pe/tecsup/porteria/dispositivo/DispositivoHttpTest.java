package pe.tecsup.porteria.dispositivo;
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
class DispositivoHttpTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios; @Autowired JwtService jwt;
    @Test void tokenSeMuestraUnaVezYSePuedeRevocar() throws Exception {
        String admin="Bearer "+jwt.generar(usuarios.findByUsername("admin.dev").orElseThrow().getId());
        var created=mvc.perform(post("/api/admin/dispositivos").header("Authorization",admin).contentType("application/json")
                .content("{\"nombre\":\"Puerta\",\"punto\":\"PEATONAL\"}")).andExpect(status().isCreated()).andReturn();
        var body=json.readTree(created.getResponse().getContentAsString());
        long id=body.get("dispositivo").get("id").asLong(); String token=body.get("token").asString();
        mvc.perform(post("/api/dispositivo/ping").header("X-Device-Token",token)).andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
        mvc.perform(get("/api/admin/dispositivos/"+id).header("Authorization",admin))
                .andExpect(jsonPath("$.tokenHash").doesNotExist()).andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.ultimoPing").isNotEmpty());
        var renewed=mvc.perform(post("/api/admin/dispositivos/"+id+"/token").header("Authorization",admin)).andExpect(status().isOk()).andReturn();
        String nuevo=json.readTree(renewed.getResponse().getContentAsString()).get("token").asString();
        mvc.perform(post("/api/dispositivo/ping").header("X-Device-Token",token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/dispositivo/ping").header("X-Device-Token",nuevo)).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/dispositivos/"+id).header("Authorization",admin)).andExpect(status().isNoContent());
        mvc.perform(post("/api/dispositivo/ping").header("X-Device-Token",nuevo)).andExpect(status().isUnauthorized());
    }
}
