package com.educktrack.configuracion.infrastructure.persistence;

import com.educktrack.configuracion.domain.ParametroInstitucional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad JPA de un parametro institucional (RF-59, RS-14).
 *
 * <p>La clave primaria es la clave del parametro y no un identificador
 * generado: no hay dos filas del mismo parametro, y hacerlo explicito en el
 * esquema evita tener que garantizarlo desde el codigo.</p>
 */
@Entity
@Table(name = "parametro_institucional")
@Getter
@Setter
@NoArgsConstructor
public class ParametroJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "clave", length = 60, nullable = false)
    private ParametroInstitucional clave;

    /**
     * Valor en texto; lo interpreta {@code ParametrosService} segun el tipo que
     * declara el parametro. Los tipos son distintos (decimal, porcentaje,
     * entero) y una columna por tipo seria peor que convertir en un solo sitio.
     */
    @Column(name = "valor", length = 100, nullable = false)
    private String valor;

    @Column(name = "actualizado", nullable = false)
    private LocalDateTime actualizado;

    /** Correo de quien lo cambio, en texto (mismo criterio que el log, V11). */
    @Column(name = "actualizado_por", length = 150, nullable = false)
    private String actualizadoPor;
}
