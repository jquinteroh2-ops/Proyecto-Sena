package com.educktrack.notificaciones.application;

import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Traduce sujetos academicos (estudiante, docente, curso) a las cuentas de
 * usuario que deben recibir el aviso (RS-08).
 *
 * <p>Existe porque una notificacion se dirige a un {@code usuario_id}, pero los
 * hechos que la provocan hablan de estudiantes, docentes y cursos. Sin este
 * paso intermedio cada listener repetiria el mismo cruce, y cada repeticion es
 * una ocasion de olvidarse del acudiente.</p>
 *
 * <p>Los perfiles sin cuenta asociada se descartan en silencio: un estudiante
 * puede estar matriculado antes de que le creen usuario (el vinculo de V9 es
 * opcional a proposito), y eso no debe impedir avisar a los demas.</p>
 */
@Service
public class DestinatariosService {

    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final VinculoAcudienteRepository vinculoRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final MatriculaRepository matriculaRepository;

    public DestinatariosService(EstudianteRepository estudianteRepository,
                                DocenteRepository docenteRepository,
                                VinculoAcudienteRepository vinculoRepository,
                                AsignacionDocenteRepository asignacionRepository,
                                MatriculaRepository matriculaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
        this.vinculoRepository = vinculoRepository;
        this.asignacionRepository = asignacionRepository;
        this.matriculaRepository = matriculaRepository;
    }

    /**
     * RS-08 / RB-08: cuenta del estudiante y las de sus acudientes vinculados.
     *
     * <p>Casi todas las alertas academicas van a este conjunto: lo que le pasa
     * a un menor le importa tambien a quien responde por el.</p>
     */
    @Transactional(readOnly = true)
    public Set<Long> delEstudianteYSusAcudientes(Long estudianteId) {
        Set<Long> destinatarios = new LinkedHashSet<>();
        cuentaDelEstudiante(estudianteId).ifPresent(destinatarios::add);
        destinatarios.addAll(cuentasDeAcudientes(estudianteId));
        return destinatarios;
    }

    /** RB-08: solo los acudientes, para avisos dirigidos a ellos (HU-07). */
    @Transactional(readOnly = true)
    public Set<Long> deLosAcudientes(Long estudianteId) {
        return new LinkedHashSet<>(cuentasDeAcudientes(estudianteId));
    }

    /**
     * RF-55: cuentas de los docentes con carga academica en un curso. Es a
     * quienes afecta el cierre de un corte, porque son los que dejan de poder
     * modificar notas.
     */
    @Transactional(readOnly = true)
    public Set<Long> deLosDocentesDelCurso(Long cursoId) {
        List<Long> docentes = asignacionRepository.findDocenteIdsByCursoId(cursoId);
        return cuentasDeDocentes(docentes);
    }

    /** RF-55: cuentas de todos los docentes con carga en el periodo indicado. */
    @Transactional(readOnly = true)
    public Set<Long> deLosDocentesDelPeriodo(Long periodoAcademicoId) {
        return cuentasDeDocentes(asignacionRepository.findDocenteIdsByPeriodoAcademicoId(periodoAcademicoId));
    }

    /**
     * RF-56: cuentas de los estudiantes activos de un curso y sus acudientes.
     * Es el destinatario natural de lo que afecta al grupo entero.
     */
    @Transactional(readOnly = true)
    public Set<Long> delCursoCompleto(Long cursoId) {
        Set<Long> destinatarios = new LinkedHashSet<>();
        for (Long estudianteId : matriculaRepository.findEstudianteIdsByCursoIdInAndEstado(
                List.of(cursoId), EstadoMatriculaCurso.ACTIVA)) {
            destinatarios.addAll(delEstudianteYSusAcudientes(estudianteId));
        }
        return destinatarios;
    }

    private java.util.Optional<Long> cuentaDelEstudiante(Long estudianteId) {
        return estudianteRepository.findById(estudianteId)
                .map(EstudianteJpaEntity::getUsuarioId)
                .filter(Objects::nonNull);
    }

    private List<Long> cuentasDeAcudientes(Long estudianteId) {
        return vinculoRepository.findByEstudianteId(estudianteId).stream()
                .map(VinculoAcudienteJpaEntity::getUsuarioId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Set<Long> cuentasDeDocentes(Collection<Long> docenteIds) {
        if (docenteIds.isEmpty()) {
            return Set.of();
        }
        return docenteRepository.findAllById(docenteIds).stream()
                .map(DocenteJpaEntity::getUsuarioId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
