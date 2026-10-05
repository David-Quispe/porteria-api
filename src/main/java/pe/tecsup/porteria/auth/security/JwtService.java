package pe.tecsup.porteria.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String ISSUER = "porteria-api";
    private final SecretKey key;
    private final Duration duracion;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${porteria.jwt.secret}") String secret,
            @Value("${porteria.jwt.duracion:PT8H}") Duration duracion) {
        this(secret, duracion, Clock.systemUTC());
    }

    JwtService(String secret, Duration duracion, Clock clock) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        if (duracion.isNegative() || duracion.isZero()) {
            throw new IllegalArgumentException("La duración del JWT debe ser positiva");
        }
        this.duracion = duracion;
        this.clock = clock;
    }

    public String generar(Long usuarioId) {
        return Jwts.builder().issuer(ISSUER).subject(usuarioId.toString())
                .issuedAt(Date.from(clock.instant()))
                .expiration(Date.from(clock.instant().plus(duracion)))
                .signWith(key).compact();
    }

    public Long validarYObtenerUsuarioId(String token) {
        String subject = Jwts.parser().verifyWith(key).requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant())).build()
                .parseSignedClaims(token).getPayload().getSubject();
        return Long.valueOf(subject);
    }

    public long duracionSegundos() {
        return duracion.toSeconds();
    }
}
