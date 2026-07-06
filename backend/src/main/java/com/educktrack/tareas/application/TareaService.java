package com.educktrack.tareas.application;

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

/**
 * Casos de uso de tareas (RF-38..RF-42). Aplica HU-23 (fecha limite no pasada)
 * y RB-10 (entrega dentro de plazo salvo autorizacion del docente).
 */
@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final EntregaTareaRepository entregaRepository;

    public TareaService(TareaRepository tareaRepository, EntregaTareaRepository entregaRepository) {
        this.tareaRepository = tareaRepository;
        this.entregaRepository = entregaRepository;
    }

    /** RF-38 / HU-23: asigna una tarea con fecha limite valida. */
    @Transactional
    public TareaDto asignar(AsignarTareaRequest req) {
        // Construir el dominio valida HU-23 (fecha limite >= hoy).
        new Tarea(req.fechaLimite(), req.permiteEntregaTardia(), LocalDate.now());
        TareaJpaEntity e = new TareaJpaEntity();
        e.setTitulo(req.titulo());
        e.setDescripcion(req.descripcion());
        e.setMateriaId(req.materiaId());
        e.setCursoId(req.cursoId());
        e.setPeriodoAcademicoId(req.periodoAcademicoId());
        e.setDocenteId(req.docenteId());
        e.setFechaLimite(req.fechaLimite());
        e.setPermiteEntregaTardia(req.permiteEntregaTardia());
        e.setFechaCreacion(LocalDateTime.now());
        return toTareaDto(tareaRepository.save(e));
    }

    @Transactional(readOnly = true)
    public List<TareaDto> listarPorCurso(Long cursoId) {
        return tareaRepository.findByCursoId(cursoId).stream().map(TareaService::toTareaDto).toList();
    }

    /** RF-39 / RB-10: registra la entrega de un estudiante dentro de plazo. */
    @Transactional
    public EntregaDto entregar(Long tareaId, EntregarTareaRequest req) {
        TareaJpaEntity tareaEntity = obtenerTarea(tareaId);
        if (entregaRepository.existsByTareaIdAndEstudianteId(tareaId, req.estudianteId())) {
            throw new ReglaNegocioException("RF-39", "El estudiante ya realizo la entrega de esta tarea.");
        }
        Tarea tarea = new Tarea(tareaEntity.getFechaLimite(), tareaEntity.isPermiteEntregaTardia(),
                tareaEntity.getFechaLimite()); // reconstruye sin re-validar HU-23
        tarea.validarEntrega(LocalDate.now()); // RB-10

        EntregaTareaJpaEntity entrega = new EntregaTareaJpaEntity();
        entrega.setTareaId(tareaId);
        entrega.setEstudianteId(req.estudianteId());
        entrega.setEvidencia(req.evidencia());
        entrega.setFechaEntrega(LocalDateTime.now());
        return toEntregaDto(entregaRepository.save(entrega));
    }

    /** RF-40: califica una entrega con retroalimentacion. */
    @Transactional
    public EntregaDto calificar(Long entregaId, CalificarTareaRequest req) {
        EntregaTareaJpaEntity entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new ReglaNegocioException("RF-40", "La entrega no existe."));
        if (req.calificacion() < 1.0 || req.calificacion() > 5.0) {
            throw new ReglaNegocioException("RB-03", "La calificacion debe estar entre 1.0 y 5.0.");
        }
        entrega.setCalificacion(req.calificacion());
        entrega.setRetroalimentacion(req.retroalimentacion());
        return toEntregaDto(entregaRepository.save(entrega));
    }

    /** RF-41: estado (pendiente/entregada/calificada) de las tareas de un curso para un estudiante. */
    @Transactional(readOnly = true)
    public List<EstadoTareaDto> estadoTareas(Long estudianteId, Long cursoId) {
        return tareaRepository.findByCursoId(cursoId).stream()
                .map(t -> new EstadoTareaDto(t.getId(), t.getTitulo(), t.getFechaLimite(),
                        estadoDe(t.getId(), estudianteId)))
                .toList();
    }

    /** RF-42: tareas proximas a vencer en los proximos {dias} dias. */
    @Transactional(readOnly = true)
    public List<TareaDto> proximasAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        return tareaRepository.findByFechaLimiteBetween(hoy, hoy.plusDays(dias))
                .stream().map(TareaService::toTareaDto).toList();
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
