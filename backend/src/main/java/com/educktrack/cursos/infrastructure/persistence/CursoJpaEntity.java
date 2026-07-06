package com.educktrack.cursos.infrastructure.persistence;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.cursos.domain.NivelEducativo;
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

/**
 * Entidad JPA de curso (RF-43). Las FK a periodo academico y docente director
 * (RB-02) se modelan como columnas de id para desacoplar los modulos; la
 * integridad referencial se declara en la migracion Flyway (RS-02).
 */
@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
public class CursoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 80, nullable = false)
    private String nombre;

    @Column(name = "grado", nullable = false)
    private int grado;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", length = 25, nullable = false)
    private NivelEducativo nivel;

    @Enumerated(EnumType.STRING)
    @Column(name = "jornada", length = 20, nullable = false)
    private Jornada jornada;

    @Column(name = "cupo_maximo", nullable = false)
    private int cupoMaximo;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;

    @Column(name = "director_grupo_id")
    private Long directorGrupoId;
}
