package pe.tecsup.porteria.dispositivo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.dispositivo.entity.Dispositivo;
import pe.tecsup.porteria.dispositivo.entity.Punto;
import pe.tecsup.porteria.dispositivo.repository.DispositivoRepository;
import pe.tecsup.porteria.dispositivo.service.DispositivoTokenService;

/**
 * Contra un PostgreSQL real con la V1 aplicada: confirma que la entidad coincide con la tabla
 * y que el token sirve para encontrar al dispositivo sin quedar guardado en claro.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DispositivoTokenIntegrationTest {

    @Autowired
    private DispositivoRepository dispositivoRepository;

    @Autowired
    private DispositivoTokenService dispositivoTokenService;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void guardaUnDispositivoYValidaSuToken() {
        Dispositivo dispositivo = new Dispositivo("Puerta principal", Punto.PEATONAL);
        String token = dispositivoTokenService.asignarNuevoToken(dispositivo);
        dispositivoRepository.saveAndFlush(dispositivo);

        assertThat(dispositivoTokenService.buscarActivoPorToken(token))
                .hasValueSatisfying(encontrado -> {
                    assertThat(encontrado.getId()).isEqualTo(dispositivo.getId());
                    assertThat(encontrado.getPunto()).isEqualTo(Punto.PEATONAL);
                    assertThat(encontrado.getCreadoEn()).isNotNull();
                });
        assertThat(dispositivoTokenService.buscarActivoPorToken(token + "x")).isEmpty();
    }

    @Test
    void unDispositivoDesactivadoNoSeReconoce() {
        Dispositivo dispositivo = new Dispositivo("Garita vehicular", Punto.VEHICULAR);
        String token = dispositivoTokenService.asignarNuevoToken(dispositivo);
        dispositivo.setActivo(false);
        dispositivoRepository.saveAndFlush(dispositivo);

        assertThat(dispositivoTokenService.buscarActivoPorToken(token)).isEmpty();
    }

    @Test
    void elTokenEnClaroNoQuedaEnLaBase() {
        Dispositivo dispositivo = new Dispositivo("Puerta lateral", Punto.PEATONAL);
        String token = dispositivoTokenService.asignarNuevoToken(dispositivo);
        dispositivoRepository.saveAndFlush(dispositivo);

        String guardado = jdbcClient.sql("SELECT token_hash FROM dispositivo WHERE id = ?")
                .param(dispositivo.getId())
                .query(String.class)
                .single();

        assertThat(guardado).hasSize(64).doesNotContain(token);
    }
}
