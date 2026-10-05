package pe.tecsup.porteria.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);
    private static final Instant AHORA = Instant.parse("2026-10-05T05:00:00Z");

    @Test
    void tokenIdentificaAlUsuarioYExpiraAlTerminarElTurno() {
        JwtService emisor = service(SECRET, AHORA);
        String token = emisor.generar(42L);
        assertThat(emisor.validarYObtenerUsuarioId(token)).isEqualTo(42L);
        assertThat(emisor.duracionSegundos()).isEqualTo(28_800);
        assertThatThrownBy(() -> service(SECRET, AHORA.plusSeconds(28_801)).validarYObtenerUsuarioId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rechazaFirmaDeOtraClaveYTextoQueNoEsJwt() {
        String otraClave = Base64.getEncoder().encodeToString("otra-clave-de-prueba-de-32-bytes!!".getBytes());
        String tokenAjeno = service(otraClave, AHORA).generar(42L);
        assertThatThrownBy(() -> service(SECRET, AHORA).validarYObtenerUsuarioId(tokenAjeno))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service(SECRET, AHORA).validarYObtenerUsuarioId("no-es-un-token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void configuracionInseguraImpideCrearElServicio() {
        assertThatThrownBy(() -> service("YWJj", AHORA)).isInstanceOf(WeakKeyException.class);
        assertThatThrownBy(() -> new JwtService(SECRET, Duration.ZERO, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private JwtService service(String secret, Instant instant) {
        return new JwtService(secret, Duration.ofHours(8), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
