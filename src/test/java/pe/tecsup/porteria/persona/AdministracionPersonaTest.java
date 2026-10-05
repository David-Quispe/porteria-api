package pe.tecsup.porteria.persona;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
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
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AdministracionPersonaTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    String bearer() { return "Bearer " + jwt.generar(usuarios.findByUsername("admin.dev").orElseThrow().getId()); }
    String persona() throws Exception {
        return json.writeValueAsString(Map.of("tipo","VISITANTE","dni","87659991","nombres","AnaAdminPrueba","apellidos","Quispe",
                "vigenciaInicio","2026-01-01"));
    }
    long crear() throws Exception {
        var r=mvc.perform(post("/api/admin/personas").header("Authorization",bearer()).contentType("application/json").content(persona()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.activo").value(true)).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void administraPersonaYNormalizaCredencial() throws Exception {
        long id=crear();
        mvc.perform(get("/api/admin/personas").param("q","AnaAdminPrueba").header("Authorization",bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(post("/api/admin/personas/"+id+"/credenciales").header("Authorization",bearer())
                .contentType("application/json").content("{\"tipo\":\"NFC\",\"valor\":\"04:ab:cd:ef\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.valor").value("04ABCDEF"));
        mvc.perform(delete("/api/admin/personas/"+id).header("Authorization",bearer())).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/personas/"+id).header("Authorization",bearer())).andExpect(jsonPath("$.activo").value(false));
    }
    @Test void rechazaDniDuplicado() throws Exception {
        crear();
        mvc.perform(post("/api/admin/personas").header("Authorization",bearer()).contentType("application/json").content(persona()))
                .andExpect(status().isConflict());
    }
    @Test void validaVigenciaPaginacionYUid() throws Exception {
        mvc.perform(get("/api/admin/personas").param("size","10000").header("Authorization",bearer())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/personas").header("Authorization",bearer()).contentType("application/json")
                .content(persona().replace("2026-01-01","2027-01-01").replace("}",",\"vigenciaFin\":\"2026-01-01\"}")))
                .andExpect(status().isBadRequest());
        long id=crear();
        mvc.perform(post("/api/admin/personas/"+id+"/credenciales").header("Authorization",bearer())
                .contentType("application/json").content("{\"tipo\":\"NFC\",\"valor\":\"no-hex\"}")).andExpect(status().isBadRequest());
    }
}
