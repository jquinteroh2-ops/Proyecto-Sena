package com.educktrack.tareas.application;

import com.educktrack.identidad.application.ContextoUsuario;
import com.educktrack.configuracion.application.ParametrosService;
import com.educktrack.notas.domain.Calificacion;
import com.educktrack.shared.domain.ReglaNegocioException;
import com.educktrack.tareas.domain.EstadoEntrega;
import com.educktrack.tareas.domain.Tarea;
import com.educktrack.tareas.infrastructure.persistence.EntregaTareaJpaEntity;
import com.educktrack.tareas.infrastructure.persistence.EntregaTareaRepository;
import com.educktrack.tareas.infrastructure.persistence.TareaJpaEntity;
import com.educktrack.tareas.infrastructure.persistence.TareaRepository;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.AsignarTareaRequest;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.CalificarTareaRequest;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.EntregaDto;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.EntregarTareaRequest;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.EstadoTareaDto;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.TareaDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Casos de uso de tareas (RF-38..RF-42). Aplica HU-23 (fecha limite no pasada)
 * y RB-10 (entrega dentro de plazo salvo autorizacion del docente).
 */
@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final EntregaTareaRepository entregaRepository;
    private final ContextoUsuario contexto;
    private final ParametrosService parametros;

    public TareaService(TareaRepository tareaRepository, EntregaTareaRepository entregaRepository,
                        ContextoUsuario contexto, ParametrosService parametros) {
        this.tareaRepository = tareaRepository;
        this.entregaRepository = entregaRepository;
        this.contexto = contexto;
        this.parametros = parametros;
    }

    /**
     * RF-38 / HU-23: asigna una tarea con fecha limite valida.
     *
     * <p>RNF-07: el docente solo asigna tareas de las materias que dicta y la
     * tarea queda registrada a su nombre. El {@code docenteId} del cuerpo se
     * ignora cuando quien llama es un docente, de modo que no puede asignar
     * tareas en nombre de un companero; Coordinacion si puede indicarlo.</p>
     */
    @Transactional
    public TareaDto asignar(AsignarTareaRequest req) {
        contexto.exigirGestionMateria(req.cursoId(), req.materiaId());
        Long docenteId = contexto.docenteIdActual().orElseGet(req::docenteId);
        // Construir el dominio valida HU-23 (fecha limite >= hoy).
        new Tarea(req.fechaLimite(), req.permiteEntregaTardia(), LocalDate.now());
        TareaJpaEntity e = new TareaJpaEntity();
        e.setTitulo(req.titulo());
        e.setDescripcion(req.descripcion());
        e.setMateriaId(req.materiaId());
        e.setCursoId(req.cursoId());
        e.setPeriodoAcademicoId(req.periodoAcademicoId());
        e.setDocenteId(docenteId);
        e.setFechaLimite(req.fechaLimite());
        e.setPermiteEntregaTardia(req.permiteEntregaTardia());
        e.setFechaCreacion(LocalDateTime.now());
        return toTareaDto(tareaRepository.save(e));
    }

    /** RNF-07: las tareas de un curso solo las ve quien alcanza ese curso. */
    @Transactional(readOnly = true)
    public List<TareaDto> listarPorCurso(Long cursoId) {
        contexto.exigirAccesoCurso(cursoId);
        return tareaRepository.findByCursoId(cursoId).stream().map(TareaService::toTareaDto).toList();
    }

    /**
     * RF-39 / RB-10: registra la entrega de un estudiante dentro de plazo.
     *
     * <p>RNF-07: cuando quien entrega es un estudiante se usa su propio
     * identificador y se descarta el del cuerpo, para que nadie pueda entregar
     * en nombre de otro.</p>
     */
    @Transactional
    public EntregaDto entregar(Long tareaId, EntregarTareaRequest req) {
        TareaJpaEntity tareaEntity = obtenerTarea(tareaId);
        Long estudianteId = contexto.resolverEstudianteId(req.estudianteId());
        if (entregaRepository.existsByTareaIdAndEstudianteId(tareaId, estudianteId)) {
            throw new ReglaNegocioException("RF-39", "El estudiante ya realizo la entrega de esta tarea.");
        }
        Tarea tarea = new Tarea(tareaEntity.getFechaLimite(), tareaEntity.isPermiteEntregaTardia(),
                tareaEntity.getFechaLimite()); // reconstruye sin re-validar HU-23
        tarea.validarEntrega(LocalDate.now()); // RB-10

        EntregaTareaJpaEntity entrega = new EntregaTareaJpaEntity();
        entrega.setTareaId(tareaId);
        entrega.setEstudianteId(estudianteId);
        entrega.setEvidencia(req.evidencia());
        entrega.setFechaEntrega(LocalDateTime.now());
        return toEntregaDto(entregaRepository.save(entrega));
    }

    /** RF-40 / RNF-07: califica una entrega de una materia que el docente dicta. */
    @Transactional
    public EntregaDto calificar(Long entregaId, CalificarTareaRequest req) {
        EntregaTareaJpaEntity entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new ReglaNegocioException("RF-40", "La entrega no existe."));
        TareaJpaEntity tarea = obtenerTarea(entrega.getTareaId());
        contexto.exigirGestionMateria(tarea.getCursoId(), tarea.getMateriaId());
        // RB-03: la escala institucional vive en el dominio de notas. Repetir
        // aqui los limites 1.0-5.0 significaba que cambiarla dejaba este punto
        // de entrada con la escala vieja.
        entrega.setCalificacion(
                new Calificacion(req.calificacion(), parametros.escalaCalificacion()).getValor());
        entrega.setRetroalimentacion(req.retroalimentacion());
        return toEntregaDto(entregaRepository.save(entrega));
    }

    /**
     * RF-41: estado (pendiente/entregada/calificada) de las tareas de un curso
     * para un estudiante.
     *
     * <p>RNF-07: el estudiante consulta siempre el suyo; el acudiente, el de
     * sus tutelados (RB-08); el docente, el de los estudiantes de sus cursos.</p>
     */
    @Transactional(readOnly = true)
    public List<EstadoTareaDto> estadoTareas(Long estudianteId, Long cursoId) {
        Long consultado = contexto.resolverEstudianteId(estudianteId);
        contexto.exigirAccesoCurso(cursoId);
        return tareaRepository.findByCursoId(cursoId).stream()
                .map(t -> new EstadoTareaDto(t.getId(), t.getTitulo(), t.getFechaLimite(),
                        estadoDe(t.getId(), consultado)))
                .toList();
    }

    /**
     * RF-42: tareas proximas a vencer en los proximos {dias} dias, acotadas a
     * los cursos que el solicitante alcanza (RNF-07). Antes de la Fase 2 este
     * listado era institucional y filtraba tareas de cursos ajenos.
     */
    @Transactional(readOnly = true)
    public List<TareaDto> proximasAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        List<TareaJpaEntity> tareas = tareaRepository.findByFechaLimiteBetween(hoy, hoy.plusDays(dias));
        if (!contexto.tieneVisionInstitucional()) {
            Set<Long> cursos = contexto.cursosVisibles();
            tareas = tareas.stream().filter(t -> cursos.contains(t.getCursoId())).toList();
        }
        return tareas.stream().map(TareaService::toTareaDto).toList();
    }

    private EstadoEntrega estadoDe(Long tareaId, Long estudianteId) {
        return entregaRepository.findByTareaIdAndEstudianteId(tareaId, estudianteId)
                .map(e -> e.getCalificacion() != null ? EstadoEntrega.CALIFICADA : EstadoEntrega.ENTREGADA)
                .orElse(EstadoEntrega.PENDIENTE);
    }

    private TareaJpaEntity obtenerTarea(Long id) {
        return tareaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-39", "La tarea no existe."));
    }

    private static TareaDto toTareaDto(TareaJpaEntity e) {
        return new TareaDto(e.getId(), e.getTitulo(), e.getDescripcion(), e.getMateriaId(), e.getCursoId(),
                e.getPeriodoAcademicoId(), e.getDocenteId(), e.getFechaLimite(), e.isPermiteEntregaTardia());
    }

    private static EntregaDto toEntregaDto(EntregaTareaJpaEntity e) {
        return new EntregaDto(e.getId(), e.getTareaId(), e.getEstudianteId(), e.getEvidencia(),
                e.getFechaEntrega(), e.getCalificacion(), e.getRetroalimentacion());
    }
}
