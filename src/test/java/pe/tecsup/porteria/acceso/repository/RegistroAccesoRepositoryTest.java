package pe.tecsup.porteria.acceso.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.acceso.entity.Direccion;
import pe.tecsup.porteria.acceso.entity.MetodoId;
import pe.tecsup.porteria.acceso.entity.RegistroAcceso;
import pe.tecsup.porteria.acceso.entity.Resultado;

/**
 * Contra un PostgreSQL real con la V1 aplicada. Los dispositivos se insertan con SQL
 * para no depender de las clases de otro módulo.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RegistroAccesoRepositoryTest {

    private static final LocalDateTime OCHO_AM = LocalDateTime.of(2026, 10, 1, 8, 0, 0);
    private static final String UID = "04A1B2C3D4E5F6";

    @Autowired
    private RegistroAccesoRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbcClient;

    private Long puertaPrincipal;
    private Long garita;

    @BeforeEach
    void crearDispositivos() {
        puertaPrincipal = insertarDispositivo("Puerta principal", "PEATONAL", "a");
        garita = insertarDispositivo("Garita", "VEHICULAR", "b");
    }

    @Test
    void guardaUnRegistroSinPersonaYLoLeeIgual() {
        RegistroAcceso guardado = repository.saveAndFlush(registro(puertaPrincipal, MetodoId.NFC, UID, OCHO_AM)
                .resultado(Resultado.NO_AUTORIZADO)
                .build());
        entityManager.clear();

        RegistroAcceso leido = repository.findById(guardado.getId()).orElseThrow();

        assertThat(leido.getPersonaId()).isNull();
        assertThat(leido.getCredencialId()).isNull();
        assertThat(leido.getDispositivoId()).isEqualTo(puertaPrincipal);
        assertThat(leido.getMetodo()).isEqualTo(MetodoId.NFC);
        assertThat(leido.getValorLeido()).isEqualTo(UID);
        assertThat(leido.getDireccion()).isEqualTo(Direccion.ENTRADA);
        assertThat(leido.getResultado()).isEqualTo(Resultado.NO_AUTORIZADO);
        assertThat(leido.getFechaHora()).isEqualTo(OCHO_AM);
    }

    @Test
    void filtroDuplicado_devuelveLaLecturaMasRecienteDelMismoValorYDispositivo() {
        guardar(puertaPrincipal, MetodoId.NFC, UID, OCHO_AM);
        RegistroAcceso masReciente = guardar(puertaPrincipal, MetodoId.NFC, UID, OCHO_AM.plusSeconds(3));
        // Lecturas más nuevas, pero de otro dispositivo, otro método u otro valor: no cuentan
        guardar(garita, MetodoId.NFC, UID, OCHO_AM.plusSeconds(4));
        guardar(puertaPrincipal, MetodoId.QR, UID, OCHO_AM.plusSeconds(4));
        guardar(puertaPrincipal, MetodoId.NFC, "FFFFFFFF", OCHO_AM.plusSeconds(4));

        assertThat(buscarReciente(puertaPrincipal, MetodoId.NFC, UID, OCHO_AM.plusSeconds(1)))
                .hasValueSatisfying(r -> assertThat(r.getId()).isEqualTo(masReciente.getId()));
    }

    @Test
    void filtroDuplicado_ignoraLecturasAnterioresALaVentana() {
        guardar(puertaPrincipal, MetodoId.NFC, UID, OCHO_AM);

        assertThat(buscarReciente(puertaPrincipal, MetodoId.NFC, UID, OCHO_AM.plusSeconds(5))).isEmpty();
    }

    @Test
    void aceptaSpecificationsParaElReporte() {
        guardar(puertaPrincipal, MetodoId.NFC, UID, OCHO_AM);
        guardar(puertaPrincipal, MetodoId.DNI, "12345678", OCHO_AM.plusMinutes(1));
        guardar(garita, MetodoId.NFC, UID, OCHO_AM.plusMinutes(2));

        Specification<RegistroAcceso> nfcEnPuertaPrincipal = (root, query, cb) -> cb.and(
                cb.equal(root.get("dispositivoId"), puertaPrincipal),
                cb.equal(root.get("metodo"), MetodoId.NFC));

        assertThat(repository.findAll(nfcEnPuertaPrincipal)).hasSize(1);
    }

    private RegistroAcceso guardar(Long dispositivoId, MetodoId metodo, String valor, LocalDateTime fechaHora) {
        return repository.saveAndFlush(registro(dispositivoId, metodo, valor, fechaHora)
                .resultado(Resultado.AUTORIZADO)
                .build());
    }

    private static RegistroAcceso.RegistroAccesoBuilder registro(
            Long dispositivoId, MetodoId metodo, String valor, LocalDateTime fechaHora) {
        return RegistroAcceso.builder()
                .dispositivoId(dispositivoId)
                .metodo(metodo)
                .valorLeido(valor)
                .direccion(Direccion.ENTRADA)
                .fechaHora(fechaHora);
    }

    private Optional<RegistroAcceso> buscarReciente(
            Long dispositivoId, MetodoId metodo, String valor, LocalDateTime desde) {
        return repository.findFirstByDispositivoIdAndMetodoAndValorLeidoAndFechaHoraGreaterThanEqualOrderByFechaHoraDesc(
                dispositivoId, metodo, valor, desde);
    }

    private Long insertarDispositivo(String nombre, String punto, String letraHash) {
        return jdbcClient.sql("INSERT INTO dispositivo (nombre, punto, token_hash) VALUES (?, ?, ?) RETURNING id")
                .params(nombre, punto, letraHash.repeat(64))
                .query(Long.class)
                .single();
    }
}
