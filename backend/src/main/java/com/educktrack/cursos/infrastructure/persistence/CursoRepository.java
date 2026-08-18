package com.educktrack.cursos.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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

    /**
     * RB-17: carga el curso bloqueando su fila hasta el fin de la transaccion.
     *
     * <p>El cupo no se puede expresar como indice unico (la Fase 3 uso columnas
     * generadas para RB-01 y RB-05, pero "no mas de N filas" no es una regla de
     * unicidad). Sin bloqueo, dos matriculas simultaneas leen el mismo recuento
     * de matriculados y ambas creen tener el ultimo cupo. Serializar sobre la
     * fila del curso es lo que hace que el recuento siga siendo valido cuando se
     * inserta.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CursoJpaEntity c where c.id = :id")
    Optional<CursoJpaEntity> findByIdParaMatricular(Long id);
}
