package pe.tecsup.porteria.acceso.entity;
import java.time.*;
import java.util.Set;
import jakarta.persistence.*;
import lombok.*;
import pe.tecsup.porteria.persona.entity.TipoPersona;
import pe.tecsup.porteria.dispositivo.entity.Punto;
@Entity @Table(name="regla_acceso") @Getter @Setter @NoArgsConstructor
public class ReglaAcceso {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name="tipo_persona",nullable=false,length=20) private TipoPersona tipoPersona;
    @Enumerated(EnumType.STRING) @Column(length=20) private Punto punto;
    @Convert(converter=DiasConverter.class) @Column(nullable=false,length=13) private Set<DayOfWeek> dias;
    @Column(name="hora_inicio",nullable=false) private LocalTime horaInicio;
    @Column(name="hora_fin",nullable=false) private LocalTime horaFin;
    @Column(length=150) private String descripcion;
    @Column(nullable=false) private boolean activa=true;
    @Column(name="creado_en",nullable=false,updatable=false) private LocalDateTime creadoEn;
    @PrePersist void crear() { creadoEn=LocalDateTime.now(); }
}
