package com.educktrack.cursos.application;

import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoRepository;
import com.educktrack.cursos.domain.Curso;
import com.educktrack.cursos.infrastructure.persistence.CursoJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.CursoMapper;
import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.CupoDto;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.CursoDto;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.EstudianteEnCursoDto;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.GuardarCursoRequest;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaJpaEntity;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de gestion de cursos (RF-43..RF-46). Aplica RB-17 (cupo) al
 * consultar disponibilidad y valida la existencia del periodo academico.
 */
@Service
public class GestionCursoService {

    private final CursoRepository cursoRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;

    public GestionCursoService(CursoRepository cursoRepository,
                               PeriodoAcademicoRepository periodoRepository,
                               MatriculaRepository matriculaRepository,
                               EstudianteRepository estudianteRepository) {
        this.cursoRepository = cursoRepository;
        this.periodoRepository = periodoRepository;
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
    }

    /** RF-43: registra un curso (el cupo > 0 lo valida el dominio, RB-17). */
    @Transactional
    public CursoDto registrar(GuardarCursoRequest req) {
        validarPeriodo(req.periodoAcademicoId());
        Curso curso = Curso.registrar(req.nombre(), req.grado(), req.nivel(), req.jornada(),
                req.cupoMaximo(), req.periodoAcademicoId());
        return toDto(cursoRepository.save(CursoMapper.toEntity(curso)));
    }

    /** RF-44: edita un curso. */
    @Transactional
    public CursoDto actualizar(Long id, GuardarCursoRequest req) {
        CursoJpaEntity e = obtener(id);
        validarPeriodo(req.periodoAcademicoId());
        if (req.cupoMaximo() <= 0) {
            throw new ReglaNegocioException("RB-17", "El cupo maximo debe ser mayor que cero.");
        }
        e.setNombre(req.nombre());
        e.setGrado(req.grado());
        e.setNivel(req.nivel());
        e.setJornada(req.jornada());
        e.setCupoMaximo(req.cupoMaximo());
        e.setPeriodoAcademicoId(req.periodoAcademicoId());
        return toDto(cursoRepository.save(e));
    }

    @Transactional(readOnly = true)
    public CursoDto consultar(Long id) {
        return toDto(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<CursoDto> listar() {
        return cursoRepository.findAll().stream().map(GestionCursoService::toDto).toList();
    }

    /** RF-46: consulta el cupo disponible de un curso (RB-17). */
    @Transactional(readOnly = true)
    public CupoDto consultarCupo(Long cursoId) {
        CursoJpaEntity curso = obtener(cursoId);
        long matriculados = matriculaRepository.countByCursoIdAndEstado(cursoId, EstadoMatriculaCurso.ACTIVA);
        long disponibles = Math.max(0, curso.getCupoMaximo() - matriculados);
        return new CupoDto(curso.getCupoMaximo(), matriculados, disponibles);
    }

    /** RF-45: lista los estudiantes matriculados en un curso con su estado. */
    @Transactional(readOnly = true)
    public List<EstudianteEnCursoDto> listarEstudiantes(Long cursoId) {
        obtener(cursoId);
        return matriculaRepository.findByCursoId(cursoId).stream()
                .map(MatriculaJpaEntity::getEstudianteId)
                .distinct()
                .map(estudianteRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(GestionCursoService::toEstudianteEnCurso)
                .toList();
    }

    private void validarPeriodo(Long periodoId) {
        if (!periodoRepository.existsById(periodoId)) {
            throw new ReglaNegocioException("RF-43", "El periodo academico indicado no existe.");
        }
    }

    private CursoJpaEntity obtener(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-44", "El curso no existe."));
    }

    private static CursoDto toDto(CursoJpaEntity e) {
        return new CursoDto(e.getId(), e.getNombre(), e.getGrado(), e.getNivel(), e.getJornada(),
                e.getCupoMaximo(), e.getPeriodoAcademicoId(), e.getDirectorGrupoId());
    }

    private static EstudianteEnCursoDto toEstudianteEnCurso(EstudianteJpaEntity e) {
        return new EstudianteEnCursoDto(e.getId(), e.getDocumento(), e.getNombres(),
                e.getApellidos(), e.getEstado());
    }
}
