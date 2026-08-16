package com.educktrack.auditoria.infrastructure.persistence;

import com.educktrack.auditoria.domain.TipoOperacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad JPA del log de auditoria (RS-07, RF-63).
 *
 * <p>El usuario se guarda como correo institucional en texto y no como clave
 * foranea: el registro debe sobrevivir al borrado de la cuenta y debe poder
 * anotar intentos de acceso con correos que ni siquiera existen.</p>
 */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario", length = 150, nullable = false)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacion", length = 40, nullable = false)
    private TipoOperacion operacion;

    /** Tabla o concepto afectado ("calificacion", "estudiante"). Nulo en accesos. */
    @Column(name = "entidad", length = 60)
    private String entidad;

    /** Identificador del registro afectado, cuando la operacion recae sobre uno. */
    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "descripcion", length = 500, nullable = false)
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
