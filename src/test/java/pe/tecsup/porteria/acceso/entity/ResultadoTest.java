package pe.tecsup.porteria.acceso.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ResultadoTest {

    @Test
    void autorizadoAbre() {
        assertThat(Resultado.AUTORIZADO.abre()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Resultado.class, names = "AUTORIZADO", mode = EnumSource.Mode.EXCLUDE)
    void ningunOtroResultadoAbre(Resultado resultado) {
        assertThat(resultado.abre()).isFalse();
    }
}
