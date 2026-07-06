package com.educktrack.horarios.application;

import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoRepository;
import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.horarios.domain.BloqueHorario;
import com.educktrack.horarios.infrastructure.persistence.AsignacionHorariaJpaEntity;
import com.educktrack.horarios.infrastructure.persistence.AsignacionHorariaRepository;
import com.educktrack.horarios.infrastructure.persistence.BloqueHorarioJpaEntity;
import com.educktrack.horarios.infrastructure.persistence.BloqueHorarioRepository;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.AsignarHorarioRequest;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.BloqueDto;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.CrearBloqueRequest;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.HorarioItemDto;
import com.educktrack.materias.infrastructure.persistence.MateriaRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de horarios (RF-21..RF-25). Concentra la validacion de cruces
 * (RB-18 / RF-23): un docente o un curso no pueden tener dos clases en el mismo
 * bloque y periodo.
 */
@Service
public class HorarioService {

    private final BloqueHorarioRepository bloqueRepository;
    private final AsignacionHorariaRepository asignacionRepository;
    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;
    private final PeriodoAcademicoRepository periodoRepository;

    public HorarioService(BloqueHorarioRepository bloqueRepository,
                          AsignacionHorariaRepository asignacionRepository,
                          MateriaRepository materiaRepository,
                          DocenteRepository docenteRepository,
                          CursoRepository cursoRepository,
                          PeriodoAcademicoRepository periodoRepository) {
        this.bloqueRepository = bloqueRepository;
        this.asignacionRepository = asignacionRepository;
        this.materiaRepository = materiaRepository;
        this.docenteRepository = docenteRepository;
        this.cursoRepository = cursoRepository;
        this.periodoRepository = periodoRepository;
    }

    /** RF-21 / HU-11: crea un bloque horario (fin>inicio via dominio; sin duplicados). */
    @Transactional
    public BloqueDto crearBloque(CrearBloqueRequest req) {
        if (bloqueRepository.existsByDiaAndHoraInicioAndHoraFinAndJornada(
                req.dia(), req.horaInicio(), req.horaFin(), req.jornada())) {
            throw new ReglaNegocioException("HU-11", "Ya existe un bloque con ese dia, hora y jornada.");
        }
        BloqueHorario bloque = BloqueHorario.crear(req.dia(), req.horaInicio(), req.horaFin(), req.jornada());
        BloqueHorarioJpaEntity e = new BloqueHorarioJpaEntity();
        e.setDia(bloque.getDia());
        e.setHoraInicio(bloque.getHoraInicio());
        e.setHoraFin(bloque.getHoraFin());
        e.setJornada(bloque.getJornada());
        return toBloqueDto(bloqueRepository.save(e));
    }

    @Transactional(readOnly = true)
    public List<BloqueDto> listarBloques() {
        return bloqueRepository.findAll().stream().map(HorarioService::toBloqueDto).toList();
    }

    /** RF-22 / RF-23 / RB-18: asigna un bloque validando cruces de docente y curso. */
    @Transactional
    public HorarioItemDto asignar(AsignarHorarioRequest req) {
        BloqueHorarioJpaEntity bloque = bloqueRepository.findById(req.bloqueId())
                .orElseThrow(() -> new ReglaNegocioException("RF-22", "El bloque horario no existe."));
        if (!materiaRepository.existsById(req.materiaId())) {
            throw new ReglaNegocioException("RF-22", "La materia no existe.");
        }
        if (!docenteRepository.existsById(req.docenteId())) {
            throw new ReglaNegocioException("RF-22", "El docente no existe.");
        }
        if (!cursoRepository.existsById(req.cursoId())) {
            throw new ReglaNegocioException("RF-22", "El curso no existe.");
        }
        if (!periodoRepository.existsById(req.periodoAcademicoId())) {
            throw new ReglaNegocioException("RF-22", "El periodo academico no existe.");
        }

        // RB-18: sin cruces para el docente ni para el curso en el mismo bloque/periodo.
        if (asignacionRepository.existsByBloqueIdAndDocenteIdAndPeriodoAcademicoId(
                req.bloqueId(), req.docenteId(), req.periodoAcademicoId())) {
            throw new ReglaNegocioException("RB-18",
                    "El docente ya tiene una clase asignada en ese bloque horario.");
        }
        if (asignacionRepository.existsByBloqueIdAndCursoIdAndPeriodoAcademicoId(
                req.bloqueId(), req.cursoId(), req.periodoAcademicoId())) {
            throw new ReglaNegocioException("RB-18",
                    "El curso ya tiene una clase asignada en ese bloque horario.");
        }

        AsignacionHorariaJpaEntity a = new AsignacionHorariaJpaEntity();
        a.setBloqueId(req.bloqueId());
        a.setMateriaId(req.materiaId());
        a.setDocenteId(req.docenteId());
        a.setCursoId(req.cursoId());
        a.setPeriodoAcademicoId(req.periodoAcademicoId());
        return toItem(asignacionRepository.save(a), bloque);
    }

    /** RF-24: horario semanal de un curso. */
    @Transactional(readOnly = true)
    public List<HorarioItemDto> horarioCurso(Long cursoId, Long periodoAcademicoId) {
        return asignacionRepository.findByCursoIdAndPeriodoAcademicoId(cursoId, periodoAcademicoId)
                .stream().map(this::toItemConBloque).toList();
    }

    /** RF-25: horario semanal de un docente. */
    @Transactional(readOnly = true)
    public List<HorarioItemDto> horarioDocente(Long docenteId, Long periodoAcademicoId) {
        return asignacionRepository.findByDocenteIdAndPeriodoAcademicoId(docenteId, periodoAcademicoId)
                .stream().map(this::toItemConBloque).toList();
    }

    private HorarioItemDto toItemConBloque(AsignacionHorariaJpaEntity a) {
        BloqueHorarioJpaEntity bloque = bloqueRepository.findById(a.getBloqueId()).orElse(null);
        return toItem(a, bloque);
    }

    private static HorarioItemDto toItem(AsignacionHorariaJpaEntity a, BloqueHorarioJpaEntity b) {
        return new HorarioItemDto(
                a.getId(), a.getBloqueId(),
                b != null ? b.getDia() : null,
                b != null ? b.getHoraInicio() : null,
                b != null ? b.getHoraFin() : null,
                b != null ? b.getJornada() : null,
                a.getMateriaId(), a.getDocenteId(), a.getCursoId());
    }

    private static BloqueDto toBloqueDto(BloqueHorarioJpaEntity e) {
        return new BloqueDto(e.getId(), e.getDia(), e.getHoraInicio(), e.getHoraFin(), e.getJornada());
    }
}
