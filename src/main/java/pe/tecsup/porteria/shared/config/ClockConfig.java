package pe.tecsup.porteria.shared.config;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.*;
@Configuration
public class ClockConfig {
    @Bean public Clock porteriaClock() { return Clock.system(ZoneId.of("America/Lima")); }
}
