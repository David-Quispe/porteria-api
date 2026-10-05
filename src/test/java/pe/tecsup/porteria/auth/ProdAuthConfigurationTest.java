package pe.tecsup.porteria.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;

@SpringBootTest(properties = {
        "JWT_SECRET=c2VjcmV0by1kZS1wcnVlYmEtc29sby1wYXJhLXByb2ZpbGUtcHJvZA==",
        "DB_URL=jdbc:postgresql://localhost/no-usada", "DB_USER=no-usado", "DB_PASSWORD=no-usado"
})
@ActiveProfiles("prod")
@Import(TestcontainersConfiguration.class)
class ProdAuthConfigurationTest {

    @Autowired UsuarioRepository usuarios;

    @Test
    void produccionNoCargaElAdministradorDeDesarrollo() {
        assertThat(usuarios.findByUsername("admin.dev")).isEmpty();
    }
}
