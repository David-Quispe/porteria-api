package pe.tecsup.porteria;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PorteriaApplication {

    public static final String ZONA_HORARIA = "America/Lima";

    public static void main(String[] args) {
        // Todas las fechas (LocalDateTime) se interpretan en hora de Lima, en cualquier máquina.
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
        SpringApplication.run(PorteriaApplication.class, args);
    }

}
