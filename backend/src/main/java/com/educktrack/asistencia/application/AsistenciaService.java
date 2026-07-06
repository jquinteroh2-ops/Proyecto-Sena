package com.educktrack.asistencia.application;

import com.educktrack.asistencia.domain.Asistencia;
import com.educktrack.asistencia.domain.EstadoAsistencia;
import com.educktrack.asistencia.infrastructure.persistence.AsistenciaJpaEntity;
import com.educktrack.asistencia.infrastructure.persistence.AsistenciaRepository;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.AsistenciaDto;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.EditarAsistenciaRequest;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.EstudianteRiesgoDto;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.ItemAsistencia;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.RegistrarAsistenciaRequest;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.ReporteAsistenciaDto;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Casos de uso de asistencia (RF-26..RF-30). Aplica RB-06 (registro unico por
 * bloque/fecha y ventana de edicion de 48h) y RB-04 (porcentaje minimo del 80%).
 */
@Service
public class AsistenciaService {

    /** RB-04: porcentaje minimo de asistencia para conservar derecho a evaluacion. */
    public static final double PORCENTAJE_MINIMO = 80.0;

    private final AsistenciaRepository asistenciaRepository;

    public AsistenciaService(AsistenciaRepository asistenciaRepository) {
        this.asistenciaRepository = asistenciaRepository;
    }

    /** RF-26 / HU-14 / RB-06: registra la asistencia del curso en un bloque y fecha. */
    @Transactional
    public List<AsistenciaDto> registrar(RegistrarAsistenciaRequest req) {
        List<AsistenciaJpaEntity> guardados = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (ItemAsistencia item : req.registros()) {
            if (asistenciaRepository.existsByEstudianteIdAndBloqueIdAndFecha(
                    item.estudianteId(), req.bloqueId(), req.fecha())) {
                throw new ReglaNegocioException("RB-06",
                        "Ya existe asistencia registrada para el estudiante " + item.estudianteId()
                                + " en ese bloque y fecha.");
            }
            AsistenciaJpaEntity a = new AsistenciaJpaEntity();
            a.setEstudianteId(item.estudianteId());
            a.setCursoId(req.cursoId());
            a.setMateriaId(req.materiaId());
            a.setBloqueId(req.bloqueId());
            a.setPeriodoAcademicoId(req.periodoAcademicoId());
            a.setFecha(req.fecha());
            a.setEstado(item.estado());
            a.setJustificada(false);
            a.setFechaRegistro(ahora);
            guardados.add(asistenciaRepository.save(a));
        }
        return guardados.stream().map(AsistenciaService::toDto).toList();
    }

    /** RF-27 / HU-15: justifica una inasistencia (no afecta el % minimo, RB-04). */
    @Transactional
    public AsistenciaDto justificar(Long id, String motivo) {
        AsistenciaJpaEntity e = obtener(id);
        Asistencia dominio = toDominio(e);
        dominio.justificar(motivo);
        e.setJustificada(dominio.isJustificada());
        e.setMotivoJustificacion(dominio.getMotivoJustificacion());
        return toDto(asistenciaRepository.save(e));
    }

    /** RF-29 / RB-06: edita el estado dentro de las 48h siguientes al registro. */
    @Transactional
    public AsistenciaDto editar(Long id, EditarAsistenciaRequest req) {
        AsistenciaJpaEntity e = obtener(id);
        Asistencia dominio = toDominio(e);
        dominio.cambiarEstado(req.estado(), LocalDateTime.now());
        e.setEstado(dominio.getEstado());
        return toDto(asistenciaRepository.save(e));
    }

    /** RF-28 / RB-04: reporte de asistencia de un estudiante en una materia y periodo. */
    @Transactional(readOnly = true)
    public ReporteAsistenciaDto reporteEstudiante(Long estudianteId, Long materiaId, Long periodoAcademicoId) {
        List<AsistenciaJpaEntity> registros = asistenciaRepository
                .findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(estudianteId, materiaId, periodoAcademicoId);
        return construirReporte(estudianteId, materiaId, periodoAcademicoId, registros);
    }

    /** RF-30 / RB-04: estudiantes de un curso/materia por debajo del minimo. */
    @Transactional(readOnly = true)
    public List<EstudianteRiesgoDto> estudiantesEnRiesgo(Long cursoId, Long materiaId, Long periodoAcademicoId) {
        Map<Long, List<AsistenciaJpaEntity>> porEstudiante = asistenciaRepository
                .findByCursoIdAndMateriaIdAndPeriodoAcademicoId(cursoId, materiaId, periodoAcademicoId)
                .stream().collect(Collectors.groupingBy(AsistenciaJpaEntity::getEstudianteId));

        List<EstudianteRiesgoDto> enRiesgo = new ArrayList<>();
        porEstudiante.forEach((estId, regs) -> {
            double porcentaje = calcularPorcentaje(regs);
            if (porcentaje < PORCENTAJE_MINIMO) {
                enRiesgo.add(new EstudianteRiesgoDto(estId, porcentaje));
            }
        });
        return enRiesgo;
    }

    private ReporteAsistenciaDto construirReporte(Long estudianteId, Long materiaId, Long periodoId,
                                                  List<AsistenciaJpaEntity> registros) {
        long total = registros.size();
        long presentes = registros.stream().filter(r -> r.getEstado() == EstadoAsistencia.PRESENTE).count();
        long tardes = registros.stream().filter(r -> r.getEstado() == EstadoAsistencia.TARDE).count();
        long ausencias = registros.stream().filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE).count();
        long ausenciasInjustificadas = registros.stream()
                .filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE && !r.isJustificada()).count();
        double porcentaje = calcularPorcentaje(registros);
        return new ReporteAsistenciaDto(estudianteId, materiaId, periodoId, total, presentes, tardes,
                ausencias, ausenciasInjustificadas, porcentaje, porcentaje < PORCENTAJE_MINIMO);
    }

    /**
     * RB-04: el porcentaje de asistencia descuenta solo las ausencias
     * injustificadas (HU-15: las justificadas no afectan el minimo).
     */
    private double calcularPorcentaje(List<AsistenciaJpaEntity> registros) {
        if (registros.isEmpty()) {
            return 100.0;
        }
        long total = registros.size();
        long ausenciasInjustificadas = registros.stream()
                .filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE && !r.isJustificada()).count();
        return Math.round((total - ausenciasInjustificadas) * 10000.0 / total) / 100.0;
    }

    private AsistenciaJpaEntity obtener(Long id) {
        return asistenciaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-29", "El registro de asistencia no existe."));
    }

    private static Asistencia toDominio(AsistenciaJpaEntity e) {
        return new Asistencia(e.getEstado(), e.isJustificada(), e.getMotivoJustificacion(), e.getFechaRegistro());
    }

    private static AsistenciaDto toDto(AsistenciaJpaEntity e) {
        return new AsistenciaDto(e.getId(), e.getEstudianteId(), e.getCursoId(), e.getMateriaId(),
                e.getBloqueId(), e.getFecha(), e.getEstado(), e.isJustificada(), e.getMotivoJustificacion());
    }
}
