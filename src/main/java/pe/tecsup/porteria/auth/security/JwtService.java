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
        return generar(usuarioId, 0);
    }

    public String generar(Long usuarioId, long version) {
        return Jwts.builder().issuer(ISSUER).subject(usuarioId.toString())
                .claim("version", version)
                .issuedAt(Date.from(clock.instant()))
                .expiration(Date.from(clock.instant().plus(duracion)))
                .signWith(key).compact();
    }

    public Long validarYObtenerUsuarioId(String token) {
        return validar(token).id();
    }

    public record IdentidadJwt(Long id, long version) {}

    public IdentidadJwt validar(String token) {
        var claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant())).build()
                .parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null) throw new io.jsonwebtoken.MalformedJwtException("Falta expiración");
        Number version = claims.get("version", Number.class);
        return new IdentidadJwt(Long.valueOf(claims.getSubject()), version == null ? 0 : version.longValue());
    }

    public long duracionSegundos() {
        return duracion.toSeconds();
    }
}
