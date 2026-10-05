package pe.tecsup.porteria.auth.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import pe.tecsup.porteria.auth.entity.Rol;
import pe.tecsup.porteria.auth.entity.Usuario;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;

/** Crea un ADMIN sólo al inicializar una base de producción vacía. */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionAdminBootstrap implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transactions;
    private final EntityManager entityManager;

    @Value("${BOOTSTRAP_ADMIN_USERNAME:}")
    private String username;
    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        transactions.executeWithoutResult(status -> {
            entityManager.createNativeQuery("select pg_advisory_xact_lock(82641001)").getResultList();
            if (usuarios.count() > 0) return;
            if (username.isBlank() || password.length() < 12
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(
                        "Base vacía: configura BOOTSTRAP_ADMIN_USERNAME y BOOTSTRAP_ADMIN_PASSWORD (12-72 bytes)");
            }
            usuarios.saveAndFlush(new Usuario(username.strip(), encoder.encode(password),
                    "Administrador", Rol.ADMIN));
        });
    }
}
