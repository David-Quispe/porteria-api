package pe.tecsup.porteria.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PROVISIONAL (Fase 0): solo deja pasar health y Swagger; todo lo demás queda cerrado.
 * <p>
 * Dueño: Dev B, que la reemplaza en B2 (JWT y rutas por rol). Dev A pide aquí, por PR,
 * el registro de DeviceTokenFilter y las rutas /api/dispositivo/** y /ws/**.
 * Es la única clase de shared que conoce a los módulos: es el punto donde se ensamblan los filtros.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/actuator/health/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
