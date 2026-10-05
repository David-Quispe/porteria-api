package pe.tecsup.porteria.shared.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import pe.tecsup.porteria.shared.exception.BusinessException;

class RateLimitServiceTest {
    @Test
    void limitaLoginPorIpYLecturasPorDispositivoSeparadamente() {
        RateLimitService limite = new RateLimitService(true);
        for (int i = 0; i < 5; i++) limite.login("127.0.0.1");
        BusinessException error = assertThrows(BusinessException.class, () -> limite.login("127.0.0.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatus());
        limite.login("127.0.0.2");
        for (int i = 0; i < 30; i++) limite.lectura(1L);
        assertThrows(BusinessException.class, () -> limite.lectura(1L));
        limite.lectura(2L);
    }
}
