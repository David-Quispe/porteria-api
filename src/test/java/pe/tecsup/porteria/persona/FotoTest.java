package pe.tecsup.porteria.persona;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;
import pe.tecsup.porteria.auth.security.JwtService;
import pe.tecsup.porteria.persona.entity.*;
import pe.tecsup.porteria.persona.repository.PersonaRepository;
import tools.jackson.databind.ObjectMapper;
@SpringBootTest(properties="porteria.fotos.directorio=target/fotos-test") @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class) @Transactional
class FotoTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt; @Autowired PersonaRepository personas;
    @Test void verificaContenidoRealYProtegeLaDescarga() throws Exception {
        long id=personas.saveAndFlush(new Persona(TipoPersona.VISITANTE,"87654321","Ana","Quispe")).getId();
        String auth="Bearer "+jwt.generar(usuarios.findByUsername("admin.dev").orElseThrow().getId());
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var file=new MockMultipartFile("file","../../foto.png","image/png",bytes.toByteArray());
        var result=mvc.perform(multipart("/api/admin/personas/"+id+"/foto").file(file).header("Authorization",auth))
                .andExpect(status().isOk()).andReturn();
        String url=json.readTree(result.getResponse().getContentAsString()).get("fotoUrl").asString();
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).header("Authorization",auth)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(multipart("/api/admin/personas/"+id+"/foto").file(new MockMultipartFile("file","falsa.png","image/png","texto".getBytes()))
                .header("Authorization",auth)).andExpect(status().isBadRequest());
    }
}
