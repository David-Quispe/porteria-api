package pe.tecsup.porteria.shared.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import pe.tecsup.porteria.shared.exception.BusinessException;

/** Bucket4j por instancia: cinco logins por IP y treinta lecturas por dispositivo cada minuto. */
@Service
public class RateLimitService {
    private final ConcurrentHashMap<String, Limite> buckets = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();
    private final boolean enabled;

    public RateLimitService(@Value("${porteria.rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void login(String ip) {
        verificar("login:" + ip, 5);
    }

    public void lectura(Long dispositivoId) {
        verificar("lectura:" + dispositivoId, 30);
    }

    private void verificar(String key, int capacity) {
        if (!enabled) return;
        long now = System.nanoTime();
        if (calls.incrementAndGet() % 1000 == 0) {
            buckets.entrySet().removeIf(entry -> now - entry.getValue().lastSeen > Duration.ofMinutes(2).toNanos());
        }
        Limite bucket = buckets.computeIfAbsent(key, ignored -> new Limite(capacity, now));
        bucket.lastSeen = now;
        if (!bucket.bucket.tryConsume(1)) throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                "Demasiadas peticiones; intenta nuevamente en un minuto");
    }

    private static final class Limite {
        private final Bucket bucket;
        private volatile long lastSeen;

        private Limite(int capacity, long now) {
            bucket = Bucket.builder().addLimit(limit -> limit.capacity(capacity)
                    .refillGreedy(capacity, Duration.ofMinutes(1))).build();
            lastSeen = now;
        }
    }
}
