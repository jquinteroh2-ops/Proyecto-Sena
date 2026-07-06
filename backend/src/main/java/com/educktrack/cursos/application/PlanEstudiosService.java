package com.educktrack.cursos.application;

import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosRepository;
import com.educktrack.materias.infrastructure.persistence.MateriaRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso del plan de estudios (RF-19, RD-05): vincula materias a un curso.
 */
@Service
public class PlanEstudiosService {

    private final PlanEstudiosRepository planRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;

    public PlanEstudiosService(PlanEstudiosRepository planRepository, CursoRepository cursoRepository,
                               MateriaRepository materiaRepository) {
        this.planRepository = planRepository;
        this.cursoRepository = cursoRepository;
        this.materiaRepository = materiaRepository;
    }

    /** RF-19: asocia una materia al plan de estudios de un curso. */
    @Transactional
    public void asociarMateria(Long cursoId, Long materiaId) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new ReglaNegocioException("RF-19", "El curso no existe.");
        }
        if (!materiaRepository.existsById(materiaId)) {
            throw new ReglaNegocioException("RF-19", "La materia no existe.");
        }
        if (planRepository.existsByCursoIdAndMateriaId(cursoId, materiaId)) {
            throw new ReglaNegocioException("RF-19", "La materia ya esta en el plan de estudios del curso.");
        }
        PlanEstudiosJpaEntity plan = new PlanEstudiosJpaEntity();
        plan.setCursoId(cursoId);
        plan.setMateriaId(materiaId);
        planRepository.save(plan);
    }

    /** Lista los ids de materia del plan de un curso. */
    @Transactional(readOnly = true)
    public List<Long> materiasDelCurso(Long cursoId) {
        return planRepository.findByCursoId(cursoId).stream()
                .map(PlanEstudiosJpaEntity::getMateriaId)
                .toList();
    }
}
