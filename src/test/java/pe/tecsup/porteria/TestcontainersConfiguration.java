package pe.tecsup.porteria;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Levanta un PostgreSQL real en Docker para las pruebas, con la misma versión que docker-compose.yml.
 * Requiere Docker Desktop encendido. Cada prueba de integración la importa con
 * {@code @Import(TestcontainersConfiguration.class)}; Spring reutiliza el mismo contenedor entre pruebas.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }

}
