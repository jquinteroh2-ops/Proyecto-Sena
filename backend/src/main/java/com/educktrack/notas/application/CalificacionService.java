package com.educktrack.notas.application;

import com.educktrack.asistencia.application.AsistenciaService;
import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosRepository;
import com.educktrack.identidad.application.ContextoUsuario;
import com.educktrack.notas.domain.Calificacion;
import com.educktrack.notas.domain.TipoEvaluacion;
import com.educktrack.notas.infrastructure.persistence.CalificacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.CalificacionRepository;
import com.educktrack.notas.infrastructure.persistence.CierreCorteJpaEntity;
import com.educktrack.notas.infrastructure.persistence.CierreCorteRepository;
import com.educktrack.notas.infrastructure.persistence.NovedadNotaJpaEntity;
import com.educktrack.notas.infrastructure.persistence.NovedadNotaRepository;
import com.educktrack.notas.infrastructure.persistence.PonderacionEvaluacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.PonderacionRepository;
import com.educktrack.notas.infrastructure.rest.NotaDtos.BoletinDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.BoletinMateriaDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.CalificacionDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.DetalleTipoDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.PromedioDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.RegistrarCalificacionRequest;
import com.educktrack.notas.domain.evento.EventosDeNotas.CorteCerrado;
import com.educktrack.notas.domain.evento.EventosDeNotas.NotaBajaRegistrada;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Casos de uso de calificaciones (RF-31..RF-37). Concentra RB-03 (escala),
 * RB-07 (promedio ponderado), RB-12 (aprobacion del periodo sobre el plan
 * completo), RB-15 (historico inmutable / novedad) y RB-19 (cierre de corte
 * bloquea modificaciones y habilita el boletin). El boletin ademas traslada
 * RB-04, que se decide en el modulo de asistencia.
 */
@Service
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final PonderacionRepository ponderacionRepository;
    private final CierreCorteRepository cierreRepository;
    private final NovedadNotaRepository novedadRepository;
    private final PlanEstudiosRepository planEstudiosRepository;
    private final AsistenciaService asistencia;
    private final ContextoUsuario contexto;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    public CalificacionService(CalificacionRepository calificacionRepository,
                               PonderacionRepository ponderacionRepository,
                               CierreCorteRepository cierreRepository,
                               NovedadNotaRepository novedadRepository,
                               PlanEstudiosRepository planEstudiosRepository,
                               AsistenciaService asistencia,
                               ContextoUsuario contexto,
                               AuditoriaService auditoria,
                               ApplicationEventPublisher eventos) {
        this.calificacionRepository = calificacionRepository;
        this.ponderacionRepository = ponderacionRepository;
        this.cierreRepository = cierreRepository;
        this.novedadRepository = novedadRepository;
        this.planEstudiosRepository = planEstudiosRepository;
        this.asistencia = asistencia;
        this.contexto = contexto;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    /**
     * RF-31 / RB-03 / HU-18: registra una calificacion en la escala 1.0-5.0.
     *
     * <p>RNF-07: el docente solo califica las materias que tiene asignadas en
     * ese curso; ser director de grupo (RB-02) da visibilidad sobre el curso
     * pero no potestad para poner notas de materias ajenas.</p>
     */
    @Transactional
    public CalificacionDto registrar(RegistrarCalificacionRequest req) {
        contexto.exigirGestionMateria(req.cursoId(), req.materiaId());
        exigirCorteAbierto(req.cursoId(), req.periodoAcademicoId());
        Calificacion nota = new Calificacion(req.valor()); // valida RB-03
        CalificacionJpaEntity e = new CalificacionJpaEntity();
        e.setEstudianteId(req.estudianteId());
        e.setMateriaId(req.materiaId());
        e.setCursoId(req.cursoId());
        e.setPeriodoAcademicoId(req.periodoAcademicoId());
        e.setTipo(req.tipo());
        e.setValor(nota.getValor());
        e.setDescripcion(req.descripcion());
        e.setFechaRegistro(LocalDateTime.now());
        CalificacionJpaEntity guardada = calificacionRepository.save(e);

        // RS-07 nombra los cambios de nota como operacion critica.
        auditoria.registrar(TipoOperacion.NOTA_REGISTRADA, "calificacion", guardada.getId(),
                "Nota " + guardada.getValor() + " (" + guardada.getTipo() + ") al estudiante "
                        + guardada.getEstudianteId() + " en la materia " + guardada.getMateriaId()
                        + ", curso " + guardada.getCursoId() + ".");

        // RB-13: el umbral lo evalua este modulo, porque la regla es de
        // calificaciones; quien escucha decide a quien avisa y con que texto.
        if (!Calificacion.esAprobatoria(guardada.getValor())) {
            eventos.publishEvent(new NotaBajaRegistrada(
                    guardada.getEstudianteId(), guardada.getMateriaId(), guardada.getCursoId(),
                    guardada.getPeriodoAcademicoId(), guardada.getValor()));
        }
        return toDto(guardada);
    }

    /** RF-32 / RB-15: edita una nota solo si el corte no esta cerrado. */
    @Transactional
    public CalificacionDto editar(Long id, BigDecimal nuevoValor) {
        CalificacionJpaEntity e = obtener(id);
        contexto.exigirGestionMateria(e.getCursoId(), e.getMateriaId()); // RNF-07
        if (cierreRepository.existsByCursoIdAndPeriodoAcademicoId(e.getCursoId(), e.getPeriodoAcademicoId())) {
            throw new ReglaNegocioException("RB-15",
                    "El corte esta cerrado; use una novedad de nota para corregir (RF-36).");
        }
        BigDecimal valorAnterior = e.getValor();
        e.setValor(new Calificacion(nuevoValor).getValor()); // RB-03
        CalificacionDto dto = toDto(calificacionRepository.save(e));

        // RS-07: el log guarda el valor anterior, porque una nota editada sin
        // saber de que valor venia no es auditable.
        auditoria.registrar(TipoOperacion.NOTA_EDITADA, "calificacion", id,
                "Nota del estudiante " + e.getEstudianteId() + " en la materia " + e.getMateriaId()
                        + " cambiada de " + valorAnterior + " a " + e.getValor() + ".");
        return dto;
    }

    /** RF-33 / RB-07 / RNF-07: promedio ponderado del estudiante en una materia/periodo. */
    @Transactional(readOnly = true)
    public PromedioDto promedio(Long estudianteId, Long materiaId, Long periodoAcademicoId) {
        contexto.exigirAccesoEstudiante(estudianteId);
        return calcularPromedio(estudianteId, materiaId, periodoAcademicoId);
    }

    /**
     * RB-07: el calculo puro del promedio, <strong>sin control de acceso</strong>.
     * Solo debe invocarse desde un metodo que ya haya autorizado al solicitante
     * sobre ese estudiante o sobre el curso completo; existe para que los
     * reportes de grupo (RF-47) no repitan la comprobacion una vez por nota.
     */
    @Transactional(readOnly = true)
    public PromedioDto calcularPromedio(Long estudianteId, Long materiaId, Long periodoAcademicoId) {
        ResultadoPromedio resultado = promedioDe(
                calificacionRepository.findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(
                        estudianteId, materiaId, periodoAcademicoId),
                ponderacionRepository.findByMateriaIdAndPeriodoAcademicoId(materiaId, periodoAcademicoId));
        return new PromedioDto(estudianteId, materiaId, periodoAcademicoId, resultado.promedio(),
                Calificacion.esAprobatoria(resultado.promedio()),
                resultado.pendientes(), resultado.detalle());
    }

    /**
     * RB-07: el calculo en si, sobre datos ya cargados y <strong>sin consultar
     * nada</strong>.
     *
     * <p>Separarlo de {@link #calcularPromedio} es lo que permite al boletin
     * resolver todas las materias del plan con las notas y ponderaciones que ya
     * trajo, en vez de volver a la base por cada asignatura.</p>
     */
    private static ResultadoPromedio promedioDe(List<CalificacionJpaEntity> notas,
                                                List<PonderacionEvaluacionJpaEntity> ponderaciones) {
        Map<TipoEvaluacion, List<CalificacionJpaEntity>> porTipo = notas.stream()
                .collect(Collectors.groupingBy(CalificacionJpaEntity::getTipo));

        List<DetalleTipoDto> detalle = new ArrayList<>();
        List<TipoEvaluacion> pendientes = new ArrayList<>();
        BigDecimal promedio = BigDecimal.ZERO;

        if (ponderaciones.isEmpty()) {
            // DECISION DE DISENO: sin ponderaciones configuradas se usa promedio simple.
            promedio = media(notas);
        } else {
            for (PonderacionEvaluacionJpaEntity p : ponderaciones) {
                List<CalificacionJpaEntity> delTipo = porTipo.get(p.getTipo());
                if (delTipo == null) {
                    pendientes.add(p.getTipo());
                    detalle.add(new DetalleTipoDto(p.getTipo(), null, p.getPorcentaje(), BigDecimal.ZERO));
                } else {
                    BigDecimal avg = media(delTipo);
                    // El aporte se acumula sin redondear y solo se redondea el
                    // total: redondear cada sumando desplazaria el promedio.
                    BigDecimal aporte = avg.multiply(BigDecimal.valueOf(p.getPorcentaje()))
                            .divide(CIEN, ESCALA_CALCULO, RoundingMode.HALF_UP);
                    promedio = promedio.add(aporte);
                    detalle.add(new DetalleTipoDto(p.getTipo(), redondear(avg), p.getPorcentaje(),
                            redondear(aporte)));
                }
            }
        }
        return new ResultadoPromedio(redondear(promedio), pendientes, detalle);
    }

    /** Calculo del promedio sin los identificadores, que solo sirven para responder. */
    private record ResultadoPromedio(BigDecimal promedio, List<TipoEvaluacion> pendientes,
                                     List<DetalleTipoDto> detalle) {
    }

    /** RF-34 / RB-19: cierra el corte de un curso/periodo (bloquea modificaciones). */
    @Transactional
    public void cerrarCorte(Long cursoId, Long periodoAcademicoId) {
        if (cierreRepository.existsByCursoIdAndPeriodoAcademicoId(cursoId, periodoAcademicoId)) {
            throw new ReglaNegocioException("RB-19", "El corte de este curso ya esta cerrado.");
        }
        CierreCorteJpaEntity c = new CierreCorteJpaEntity();
        c.setCursoId(cursoId);
        c.setPeriodoAcademicoId(periodoAcademicoId);
        c.setFechaCierre(LocalDateTime.now());
        c.setCerradoPor(usuarioActual());
        cierreRepository.save(c);

        // HU-20: "el cierre queda registrado en el log de auditoria".
        auditoria.registrar(TipoOperacion.CORTE_CERRADO, "cierre_corte", c.getId(),
                "Cierre del corte del curso " + cursoId + " en el periodo " + periodoAcademicoId
                        + ". A partir de aqui las notas solo se corrigen por novedad (RB-15).");

        // RF-55 y RF-56: el cierre bloquea las notas para los docentes y a la
        // vez habilita el boletin para estudiantes y acudientes (RB-19).
        eventos.publishEvent(new CorteCerrado(cursoId, periodoAcademicoId));
    }

    /** RF-36 / RB-15: correccion auditada de una nota de un corte cerrado. */
    @Transactional
    public CalificacionDto registrarNovedad(Long calificacionId, BigDecimal nuevoValor, String motivo) {
        CalificacionJpaEntity e = obtener(calificacionId);
        if (!cierreRepository.existsByCursoIdAndPeriodoAcademicoId(e.getCursoId(), e.getPeriodoAcademicoId())) {
            throw new ReglaNegocioException("RB-15",
                    "La novedad solo aplica a cortes cerrados; edite la nota directamente (RF-32).");
        }
        BigDecimal valorAnterior = e.getValor();
        BigDecimal valorNuevo = new Calificacion(nuevoValor).getValor(); // RB-03

        NovedadNotaJpaEntity n = new NovedadNotaJpaEntity();
        n.setCalificacionId(calificacionId);
        n.setValorAnterior(valorAnterior);
        n.setValorNuevo(valorNuevo);
        n.setMotivo(motivo);
        n.setUsuario(usuarioActual());
        n.setFecha(LocalDateTime.now());
        novedadRepository.save(n);

        e.setValor(valorNuevo);
        CalificacionDto dto = toDto(calificacionRepository.save(e));

        // HU-22: la correccion sobre un corte cerrado deja rastro en dos sitios,
        // 'novedad_nota' (el historico que ve el docente, RB-15) y el log de
        // auditoria (RS-07). Son publicos distintos, no una duplicacion.
        auditoria.registrar(TipoOperacion.NOTA_NOVEDAD, "calificacion", calificacionId,
                "Novedad sobre corte cerrado: nota del estudiante " + e.getEstudianteId()
                        + " corregida de " + valorAnterior + " a " + valorNuevo + ". Motivo: " + motivo + ".");
        return dto;
    }

    /**
     * RF-35 / RB-19 / RD-11 / RNF-07: genera el boletin de un estudiante (corte cerrado).
     *
     * <p>RB-11 / RB-12: el boletin se arma sobre <strong>el plan de estudios del
     * curso</strong>, no sobre las materias que resultan tener notas. Matricular
     * a un estudiante lo inscribe en todas las materias del plan (RB-11), asi
     * que una materia del plan sin ninguna nota es informacion que falta, no una
     * materia que no exista: listarla es lo que permite que RB-12 signifique
     * "aprobo todas las materias del plan" y no "aprobo aquellas en las que
     * alguien alcanzo a calificarle".</p>
     */
    @Transactional(readOnly = true)
    public BoletinDto boletin(Long estudianteId, Long cursoId, Long periodoAcademicoId) {
        contexto.exigirAccesoEstudiante(estudianteId);
        if (!cierreRepository.existsByCursoIdAndPeriodoAcademicoId(cursoId, periodoAcademicoId)) {
            throw new ReglaNegocioException("RB-19",
                    "El boletin solo puede generarse cuando el corte del curso esta cerrado.");
        }
        List<CalificacionJpaEntity> notas = calificacionRepository
                .findByEstudianteIdAndPeriodoAcademicoId(estudianteId, periodoAcademicoId);
        Map<Long, List<CalificacionJpaEntity>> porMateria = notas.stream()
                .collect(Collectors.groupingBy(CalificacionJpaEntity::getMateriaId));

        List<Long> materiasDelBoletin = materiasDelBoletin(cursoId, porMateria.keySet());

        // Las tres consultas que necesita el boletin se hacen aqui, una vez cada
        // una, en vez de una por materia. Con un plan de diez asignaturas eso
        // pasaba de una treintena de consultas a tres.
        Map<Long, List<PonderacionEvaluacionJpaEntity>> ponderacionesPorMateria =
                materiasDelBoletin.isEmpty() ? Map.of()
                        : ponderacionRepository
                        .findByPeriodoAcademicoIdAndMateriaIdIn(periodoAcademicoId, materiasDelBoletin)
                        .stream()
                        .collect(Collectors.groupingBy(PonderacionEvaluacionJpaEntity::getMateriaId));
        Set<Long> sinDerechoAEvaluacion =
                asistencia.materiasSinDerechoAEvaluacion(estudianteId, periodoAcademicoId);

        List<BoletinMateriaDto> materias = new ArrayList<>();
        for (Long materiaId : materiasDelBoletin) {
            boolean sinCalificar = !porMateria.containsKey(materiaId);
            // El acceso ya quedo autorizado al entrar al boletin (RNF-07).
            BigDecimal prom = sinCalificar ? Calificacion.normalizar(BigDecimal.ZERO)
                    : promedioDe(porMateria.get(materiaId),
                    ponderacionesPorMateria.getOrDefault(materiaId, List.of())).promedio();
            // RB-04: el boletin informa de la perdida del derecho a evaluacion
            // ordinaria, pero no la convierte en reprobacion. Son reglas
            // distintas y mezclarlas haria imposible distinguir a quien perdio
            // la materia de quien perdio la asistencia.
            materias.add(new BoletinMateriaDto(materiaId, prom,
                    !sinCalificar && Calificacion.esAprobatoria(prom), sinCalificar,
                    sinDerechoAEvaluacion.contains(materiaId)));
        }
        BigDecimal promedioGeneral = redondear(mediaDe(
                materias.stream().map(BoletinMateriaDto::promedio).toList()));
        // RB-12: aprueba si todas las materias son aprobatorias.
        boolean aprobado = !materias.isEmpty() && materias.stream().allMatch(BoletinMateriaDto::aprobada);
        return new BoletinDto(estudianteId, cursoId, periodoAcademicoId, materias, promedioGeneral, aprobado);
    }

    /**
     * Materias que debe listar el boletin: las del plan de estudios del curso
     * (RB-11) mas cualquier materia con notas que no figure en el plan.
     *
     * <p>Esas notas huerfanas se incluyen a proposito. Aparecen cuando el plan
     * se modifica a mitad de periodo, y ocultarlas haria desaparecer del boletin
     * notas que el estudiante si tiene. Si el curso no tiene plan definido, el
     * boletin queda como estaba: solo lo calificado.</p>
     */
    private List<Long> materiasDelBoletin(Long cursoId, Set<Long> materiasConNotas) {
        Set<Long> materias = new LinkedHashSet<>(planEstudiosRepository.findByCursoId(cursoId).stream()
                .map(PlanEstudiosJpaEntity::getMateriaId).toList());
        materias.addAll(materiasConNotas);
        return List.copyOf(materias);
    }

    /** RF-37 / RNF-07: historico completo de calificaciones de un estudiante. */
    @Transactional(readOnly = true)
    public List<CalificacionDto> historico(Long estudianteId) {
        contexto.exigirAccesoEstudiante(estudianteId);
        return calificacionRepository.findByEstudianteId(estudianteId).stream()
                .map(CalificacionService::toDto).toList();
    }

    private void exigirCorteAbierto(Long cursoId, Long periodoAcademicoId) {
        if (cierreRepository.existsByCursoIdAndPeriodoAcademicoId(cursoId, periodoAcademicoId)) {
            throw new ReglaNegocioException("RB-19",
                    "El corte del curso esta cerrado; no se pueden registrar notas.");
        }
    }

    private CalificacionJpaEntity obtener(Long id) {
        return calificacionRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-32", "La calificacion no existe."));
    }

    private static String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    /** Escala intermedia del calculo, mas fina que la de presentacion. */
    private static final int ESCALA_CALCULO = 6;
    private static final BigDecimal CIEN = new BigDecimal("100");

    /** Lleva un valor a los dos decimales con que se presentan las notas (RB-03). */
    private static BigDecimal redondear(BigDecimal v) {
        return v.setScale(Calificacion.ESCALA, RoundingMode.HALF_UP);
    }

    /** Media aritmetica de un conjunto de notas, sin redondear todavia. */
    private static BigDecimal media(List<CalificacionJpaEntity> notas) {
        return mediaDe(notas.stream().map(CalificacionJpaEntity::getValor).toList());
    }

    private static BigDecimal mediaDe(List<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), ESCALA_CALCULO, RoundingMode.HALF_UP);
    }

    private static CalificacionDto toDto(CalificacionJpaEntity e) {
        return new CalificacionDto(e.getId(), e.getEstudianteId(), e.getMateriaId(), e.getCursoId(),
                e.getPeriodoAcademicoId(), e.getTipo(), e.getValor(), e.getDescripcion());
    }
}
