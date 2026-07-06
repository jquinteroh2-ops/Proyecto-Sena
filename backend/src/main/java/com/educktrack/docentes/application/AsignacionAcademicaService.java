package com.educktrack.docentes.application;

import com.educktrack.cursos.domain.Curso;
import com.educktrack.cursos.infrastructure.persistence.CursoJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.CursoMapper;
import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.docentes.domain.Docente;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteMapper;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.AsignacionDto;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.AsignarMateriaRequest;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.CargaAcademicaDto;
import com.educktrack.materias.infrastructure.persistence.MateriaJpaEntity;
import com.educktrack.materias.infrastructure.persistence.MateriaRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de asignacion academica de docentes (RF-14, RF-15, RF-16).
 * Aplica RB-16 (materia acorde al area del docente) y RB-02 (director unico).
 */
@Service
public class AsignacionAcademicaService {

    private final AsignacionDocenteRepository asignacionRepository;
    private final DocenteRepository docenteRepository;
    private final MateriaRepository materiaRepository;
    private final CursoRepository cursoRepository;

    public AsignacionAcademicaService(AsignacionDocenteRepository asignacionRepository,
                                      DocenteRepository docenteRepository,
                                      MateriaRepository materiaRepository,
                                      CursoRepository cursoRepository) {
        this.asignacionRepository = asignacionRepository;
        this.docenteRepository = docenteRepository;
        this.materiaRepository = materiaRepository;
        this.cursoRepository = cursoRepository;
    }

    /** RF-14 / RB-16: asigna una materia acorde al area de formacion del docente. */
    @Transactional
    public AsignacionDto asignarMateria(AsignarMateriaRequest req) {
        DocenteJpaEntity docenteEntity = docenteRepository.findById(req.docenteId())
                .orElseThrow(() -> new ReglaNegocioException("RF-14", "El docente no existe."));
        MateriaJpaEntity materia = materiaRepository.findById(req.materiaId())
                .orElseThrow(() -> new ReglaNegocioException("RF-14", "La materia no existe."));
        if (!cursoRepository.existsById(req.cursoId())) {
            throw new ReglaNegocioException("RF-14", "El curso no existe.");
        }

        // RB-16: la materia debe corresponder a un area de formacion del docente.
        Docente docente = DocenteMapper.toDomain(docenteEntity);
        if (!docente.puedeDictarArea(materia.getArea())) {
            throw new ReglaNegocioException("RB-16",
                    "El docente no puede dictar materias del area '" + materia.getArea() + "'.");
        }

        if (asignacionRepository.existsByDocenteIdAndMateriaIdAndCursoIdAndPeriodoAcademicoId(
                req.docenteId(), req.materiaId(), req.cursoId(), req.periodoAcademicoId())) {
            throw new ReglaNegocioException("RF-14", "La asignacion ya existe.");
        }

        AsignacionDocenteJpaEntity a = new AsignacionDocenteJpaEntity();
        a.setDocenteId(req.docenteId());
        a.setMateriaId(req.materiaId());
        a.setCursoId(req.cursoId());
        a.setPeriodoAcademicoId(req.periodoAcademicoId());
        AsignacionDocenteJpaEntity guardada = asignacionRepository.save(a);
        return new AsignacionDto(guardada.getId(), guardada.getDocenteId(), guardada.getMateriaId(),
                guardada.getCursoId(), guardada.getPeriodoAcademicoId());
    }

    /** RF-15: consulta la carga academica (materias y horas) de un docente en un periodo. */
    @Transactional(readOnly = true)
    public CargaAcademicaDto consultarCarga(Long docenteId, Long periodoAcademicoId) {
        List<AsignacionDocenteJpaEntity> asignaciones =
                asignacionRepository.findByDocenteIdAndPeriodoAcademicoId(docenteId, periodoAcademicoId);
        int totalHoras = asignaciones.stream()
                .map(a -> materiaRepository.findById(a.getMateriaId())
                        .map(MateriaJpaEntity::getIntensidadHorariaSemanal).orElse(0))
                .reduce(0, Integer::sum);
        return new CargaAcademicaDto(docenteId, periodoAcademicoId, asignaciones.size(), totalHoras);
    }

    /** RF-16 / RB-02: designa al docente como director de grupo (director unico del curso). */
    @Transactional
    public void designarDirector(Long cursoId, Long docenteId) {
        CursoJpaEntity cursoEntity = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new ReglaNegocioException("RF-16", "El curso no existe."));
        if (!docenteRepository.existsById(docenteId)) {
            throw new ReglaNegocioException("RF-16", "El docente no existe.");
        }
        // RB-02: el curso tiene exactamente un director; se asigna/actualiza el unico campo.
        Curso curso = CursoMapper.toDomain(cursoEntity);
        curso.designarDirectorGrupo(docenteId);
        cursoEntity.setDirectorGrupoId(curso.getDirectorGrupoId());
        cursoRepository.save(cursoEntity);
    }
}
