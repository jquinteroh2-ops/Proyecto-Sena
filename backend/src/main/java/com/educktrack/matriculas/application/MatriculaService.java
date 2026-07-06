package com.educktrack.matriculas.application;

import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoRepository;
import com.educktrack.cursos.domain.Curso;
import com.educktrack.cursos.infrastructure.persistence.CursoJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.CursoMapper;
import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosRepository;
import com.educktrack.estudiantes.domain.EstadoMatricula;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaJpaEntity;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
import com.educktrack.matriculas.infrastructure.rest.MatriculaDtos.MatriculaDto;
import com.educktrack.matriculas.infrastructure.rest.MatriculaDtos.MatricularRequest;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Caso de uso de matricula (RF-09 / HU-06). Concentra las reglas de negocio:
 * unicidad de matricula activa por periodo (RB-01), cupo maximo del curso
 * (RB-17) e inscripcion implicita en todas las materias del plan (RB-11).
 */
@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;
    private final CursoRepository cursoRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final PlanEstudiosRepository planEstudiosRepository;

    public MatriculaService(MatriculaRepository matriculaRepository,
                            EstudianteRepository estudianteRepository,
                            CursoRepository cursoRepository,
                            PeriodoAcademicoRepository periodoRepository,
                            PlanEstudiosRepository planEstudiosRepository) {
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
        this.cursoRepository = cursoRepository;
        this.periodoRepository = periodoRepository;
        this.planEstudiosRepository = planEstudiosRepository;
    }

    @Transactional
    public MatriculaDto matricular(MatricularRequest req) {
        EstudianteJpaEntity estudiante = estudianteRepository.findById(req.estudianteId())
                .orElseThrow(() -> new ReglaNegocioException("RF-09", "El estudiante no existe."));
        if (estudiante.getEstado() != EstadoMatricula.ACTIVO) {
            throw new ReglaNegocioException("RF-09", "Solo se pueden matricular estudiantes activos.");
        }

        CursoJpaEntity cursoEntity = cursoRepository.findById(req.cursoId())
                .orElseThrow(() -> new ReglaNegocioException("RF-09", "El curso no existe."));
        if (!periodoRepository.existsById(req.periodoAcademicoId())) {
            throw new ReglaNegocioException("RF-09", "El periodo academico no existe.");
        }

        // RB-01: un estudiante solo puede tener una matricula activa por periodo.
        if (matriculaRepository.existsByEstudianteIdAndPeriodoAcademicoIdAndEstado(
                req.estudianteId(), req.periodoAcademicoId(), EstadoMatriculaCurso.ACTIVA)) {
            throw new ReglaNegocioException("RB-01",
                    "El estudiante ya tiene una matricula activa en este periodo academico.");
        }

        // RB-17: no exceder el cupo maximo del curso (validacion en el dominio).
        Curso curso = CursoMapper.toDomain(cursoEntity);
        long matriculados = matriculaRepository.countByCursoIdAndEstado(req.cursoId(), EstadoMatriculaCurso.ACTIVA);
        curso.validarCupo((int) matriculados);

        MatriculaJpaEntity matricula = new MatriculaJpaEntity();
        matricula.setEstudianteId(req.estudianteId());
        matricula.setCursoId(req.cursoId());
        matricula.setPeriodoAcademicoId(req.periodoAcademicoId());
        matricula.setFechaMatricula(LocalDate.now());
        matricula.setEstado(EstadoMatriculaCurso.ACTIVA);
        MatriculaJpaEntity guardada = matriculaRepository.save(matricula);

        // RB-11: el estudiante queda inscrito en todas las materias del plan del curso.
        int materiasInscritas = planEstudiosRepository.findByCursoId(req.cursoId()).size();
        return toDto(guardada, materiasInscritas);
    }

    /** RB-01 admite volver a matricular tras un retiro: marca la activa como RETIRADA. */
    @Transactional
    public void anularMatricula(Long matriculaId) {
        MatriculaJpaEntity m = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new ReglaNegocioException("RF-09", "La matricula no existe."));
        m.setEstado(EstadoMatriculaCurso.RETIRADA);
        matriculaRepository.save(m);
    }

    private static MatriculaDto toDto(MatriculaJpaEntity m, int materiasInscritas) {
        return new MatriculaDto(m.getId(), m.getEstudianteId(), m.getCursoId(),
                m.getPeriodoAcademicoId(), m.getFechaMatricula(), m.getEstado(), materiasInscritas);
    }
}
