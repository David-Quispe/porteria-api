package pe.tecsup.porteria.acceso.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una lectura procesada en portería. Es historial: se crea una vez y no se modifica.
 * <p>
 * Dispositivo, persona y credencial son de otros módulos, así que se guardan solo sus ids
 * (docs/decisiones-fase0.md §4, "Referencias entre módulos").
 */
@Entity
@Table(name = "registro_acceso")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "dispositivo_id", nullable = false)
    private Long dispositivoId;

    /** NULL cuando nadie tiene la credencial leída (NO_AUTORIZADO). */
    @Column(name = "persona_id")
    private Long personaId;

    /** NULL con el método DNI, con credencial desconocida o si la credencial se eliminó después. */
    @Column(name = "credencial_id")
    private Long credencialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetodoId metodo;

    /** Lo que leyó el dispositivo, ya normalizado. Se conserva aunque la credencial se elimine. */
    @Column(name = "valor_leido", nullable = false, length = 100)
    private String valorLeido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direccion direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Resultado resultado;
}
