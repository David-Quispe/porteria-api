package pe.tecsup.porteria.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.DispatcherType;
import pe.tecsup.porteria.auth.security.JwtFilter;
import pe.tecsup.porteria.auth.security.JwtService;
import pe.tecsup.porteria.auth.service.UsuarioService;

/**
 * Ensambla las cadenas del panel y el dispositivo. La conexión STOMP
 * autentica su JWT en el frame CONNECT dentro de AccesoWebSocketAuth.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain dispositivos(HttpSecurity http,
            pe.tecsup.porteria.dispositivo.service.DispositivoTokenService tokens, SecurityErrorHandler errors) throws Exception {
        return http.securityMatcher("/api/dispositivo/**")
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(a -> a.anyRequest().hasAuthority("DEVICE"))
                .addFilterBefore(new pe.tecsup.porteria.dispositivo.security.DeviceTokenFilter(tokens, errors),
                        UsernamePasswordAuthenticationFilter.class).build();
    }

    private static final String[] RUTAS_PUBLICAS = {
            "/actuator/health/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    @org.springframework.core.annotation.Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
            UsuarioService usuarioService, SecurityErrorHandler errors) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(auth -> auth
                        // StreamingResponseBody continues on an async dispatch after the initial JWT check.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/fotos/**").hasAnyRole("ADMIN", "PORTERO")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/portero/**").hasAnyRole("ADMIN", "PORTERO")
                        .anyRequest().denyAll())
                .addFilterBefore(new JwtFilter(jwtService, usuarioService, errors),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
