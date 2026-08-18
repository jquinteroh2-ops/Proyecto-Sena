package com.educktrack.seguridad.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad JPA de un enlace de recuperacion de contrasena (RF-64, HU-04).
 *
 * <p>Guarda el <strong>hash</strong> del token, nunca el token: mientras esta
 * vivo, el enlace permite tomar el control de la cuenta sin conocer la
 * contrasena, de modo que es una credencial y se trata como tal (RS-05).</p>
 */
@Entity
@Table(name = "token_recuperacion")
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** SHA-256 en hexadecimal del token entregado al usuario. */
    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /**
     * Momento en que se consumio el enlace. {@code null} significa "sin usar":
     * HU-04 exige que expire automaticamente al usarse una vez. No se borra la
     * fila al consumirla, porque un token gastado que reaparece es justo lo que
     * interesa poder auditar.
     */
    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;
}
