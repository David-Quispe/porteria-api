package pe.tecsup.porteria.dispositivo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.dispositivo.entity.Dispositivo;
import pe.tecsup.porteria.dispositivo.repository.DispositivoRepository;

/**
 * Tokens de los ESP32. El token se entrega en claro una sola vez; en la base solo queda su SHA-256.
 * <p>
 * Se usa SHA-256 y no BCrypt porque hay que <em>buscar</em> el dispositivo por su token,
 * y BCrypt da un hash distinto cada vez. El token tiene 256 bits aleatorios, así que no necesita sal.
 */
@Service
@RequiredArgsConstructor
public class DispositivoTokenService {

    private static final int BYTES_TOKEN = 32;

    private final DispositivoRepository dispositivoRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Genera un token nuevo, guarda su hash en el dispositivo (sin persistirlo) y devuelve el token en claro.
     * Es la única vez que el token se puede ver: al registrar o al regenerar.
     */
    public String asignarNuevoToken(Dispositivo dispositivo) {
        byte[] bytes = new byte[BYTES_TOKEN];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        dispositivo.setTokenHash(hash(token));
        return token;
    }

    /**
     * Dispositivo activo dueño del token. Vacío si el token no llega, no existe o el dispositivo está desactivado.
     */
    @Transactional(readOnly = true)
    public Optional<Dispositivo> buscarActivoPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return dispositivoRepository.findByTokenHashAndActivoTrue(hash(token.strip()));
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM trae SHA-256; si falta, la instalación de Java está rota.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
