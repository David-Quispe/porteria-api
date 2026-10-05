package pe.tecsup.porteria.dispositivo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un circuito ESP32 registrado en portería. Se autentica con el header X-Device-Token.
 */
@Entity
@Table(name = "dispositivo")
@Getter
@Setter
@NoArgsConstructor
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Punto punto;

    /** SHA-256 del token en hex. El token en claro nunca se guarda. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_ping")
    private LocalDateTime ultimoPing;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Dispositivo(String nombre, Punto punto) {
        this.nombre = nombre;
        this.punto = punto;
    }

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
    }
}
