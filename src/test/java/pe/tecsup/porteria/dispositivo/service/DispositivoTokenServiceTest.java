package pe.tecsup.porteria.dispositivo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.tecsup.porteria.dispositivo.entity.Dispositivo;
import pe.tecsup.porteria.dispositivo.entity.Punto;
import pe.tecsup.porteria.dispositivo.repository.DispositivoRepository;

@ExtendWith(MockitoExtension.class)
class DispositivoTokenServiceTest {

    @Mock
    private DispositivoRepository dispositivoRepository;

    @InjectMocks
    private DispositivoTokenService service;

    @Test
    void asignarNuevoToken_guardaSoloElHashEnElDispositivo() {
        Dispositivo dispositivo = new Dispositivo("Puerta principal", Punto.PEATONAL);

        String token = service.asignarNuevoToken(dispositivo);

        // 32 bytes en Base64URL sin relleno = 43 caracteres seguros para un header
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(dispositivo.getTokenHash())
                .matches("[0-9a-f]{64}")
                .isNotEqualTo(token)
                .isEqualTo(DispositivoTokenService.hash(token));
    }

    @Test
    void asignarNuevoToken_nuncaRepiteTokens() {
        Set<String> tokens = IntStream.range(0, 200)
                .mapToObj(i -> service.asignarNuevoToken(new Dispositivo()))
                .collect(Collectors.toSet());

        assertThat(tokens).hasSize(200);
    }

    @Test
    void hash_esSha256EnHexMinusculas() {
        // Vector de prueba estándar de SHA-256 (FIPS 180-2)
        assertThat(DispositivoTokenService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void buscarActivoPorToken_sinToken_noConsultaLaBase(String token) {
        assertThat(service.buscarActivoPorToken(token)).isEmpty();

        verifyNoInteractions(dispositivoRepository);
    }

    @Test
    void buscarActivoPorToken_buscaPorElHashDelToken() {
        Dispositivo dispositivo = new Dispositivo("Garita vehicular", Punto.VEHICULAR);
        String token = service.asignarNuevoToken(dispositivo);
        when(dispositivoRepository.findByTokenHashAndActivoTrue(dispositivo.getTokenHash()))
                .thenReturn(Optional.of(dispositivo));

        assertThat(service.buscarActivoPorToken(" " + token + " ")).containsSame(dispositivo);
    }
}
