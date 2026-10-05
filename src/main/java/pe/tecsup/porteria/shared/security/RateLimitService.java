package pe.tecsup.porteria.shared.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import pe.tecsup.porteria.shared.exception.BusinessException;

/** Límite por instancia: cinco logins por IP y treinta lecturas por dispositivo cada minuto. */
@Service
public class RateLimitService {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
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
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, now));
        synchronized (bucket) {
            bucket.lastSeen = now;
            double refill = (now - bucket.lastRefill) * capacity / (double) Duration.ofMinutes(1).toNanos();
            bucket.tokens = Math.min(capacity, bucket.tokens + Math.max(0, refill));
            bucket.lastRefill = now;
            if (bucket.tokens < 1) throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas peticiones; intenta nuevamente en un minuto");
            bucket.tokens -= 1;
        }
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefill;
        private volatile long lastSeen;

        private Bucket(int capacity, long now) {
            tokens = capacity;
            lastRefill = now;
            lastSeen = now;
        }
    }
}
