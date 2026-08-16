package com.educktrack.docentes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositorio Spring Data de asignaciones academicas (RF-14, RF-15).
 */
public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocenteJpaEntity, Long> {

    List<AsignacionDocenteJpaEntity> findByDocenteIdAndPeriodoAcademicoId(Long docenteId, Long periodoAcademicoId);

    List<AsignacionDocenteJpaEntity> findByCursoId(Long cursoId);

    boolean existsByDocenteIdAndMateriaIdAndCursoIdAndPeriodoAcademicoId(
            Long docenteId, Long materiaId, Long cursoId, Long periodoAcademicoId);

    /**
     * RNF-07: cursos sobre los que el docente ejerce carga academica, sin
     * distinguir materia ni periodo. Es una de las dos vias por las que un
     * docente alcanza a un estudiante; la otra es la direccion de grupo (RB-02).
     */
    @Query("select distinct a.cursoId from AsignacionDocenteJpaEntity a where a.docenteId = :docenteId")
    List<Long> findCursoIdsByDocenteId(Long docenteId);

    /** RNF-07: la materia acota ademas el alcance en las consultas por materia. */
    boolean existsByDocenteIdAndCursoIdAndMateriaId(Long docenteId, Long cursoId, Long materiaId);
}
