package com.educktrack.cursos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositorio Spring Data de cursos (RF-43..RF-46).
 */
public interface CursoRepository extends JpaRepository<CursoJpaEntity, Long> {

    List<CursoJpaEntity> findByPeriodoAcademicoId(Long periodoAcademicoId);

    /**
     * RB-02 / RNF-07: cursos de los que el docente es director de grupo. Un
     * director de grupo alcanza a sus estudiantes aunque no tenga ninguna
     * materia asignada en ese curso.
     */
    @Query("select c.id from CursoJpaEntity c where c.directorGrupoId = :docenteId")
    List<Long> findIdsByDirectorGrupoId(Long docenteId);
}
