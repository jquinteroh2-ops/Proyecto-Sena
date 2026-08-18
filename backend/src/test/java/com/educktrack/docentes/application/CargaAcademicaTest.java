package com.educktrack.docentes.application;

import com.educktrack.configuracion.application.ParametrosService;
import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.AsignarMateriaRequest;
import com.educktrack.materias.infrastructure.persistence.MateriaJpaEntity;
import com.educktrack.materias.infrastructure.persistence.MateriaRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de RB-09: un docente no puede exceder el maximo de horas semanales
 * que define la institucion.
 *
 * <p>La regla se comprueba al asignar (RF-14) y no al consultar la carga
 * (RF-15), porque asignar es el unico momento en que la carga crece: informar
 * despues seria informar de un exceso ya consumado.</p>
 */
@ExtendWith(MockitoExtension.class)
class CargaAcademicaTest {

    private static final int MAX_HORAS = 30;
    private static final Long DOCENTE = 55L;
    private static final Long CURSO = 3L;
    private static final Long PERIODO = 1L;
    private static final Long MATEMATICAS = 7L;
    private static final String AREA = "MATEMATICAS";

    @Mock private AsignacionDocenteRepository asignacionRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private MateriaRepository materiaRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private ParametrosService parametros;

    private AsignacionAcademicaService service;

    @BeforeEach
    void prepararEscenario() {
        // Desde la Fase 9 el maximo es un parametro institucional (RF-59), no
        // una propiedad del despliegue: lo entrega ParametrosService.
        service = new AsignacionAcademicaService(asignacionRepository, docenteRepository,
                materiaRepository, cursoRepository, parametros);
        lenient().when(parametros.maxHorasDocente()).thenReturn(MAX_HORAS);

        lenient().when(docenteRepository.findById(DOCENTE)).thenReturn(Optional.of(docente()));
        lenient().when(cursoRepository.existsById(CURSO)).thenReturn(true);
        lenient().when(asignacionRepository.existsByDocenteIdAndMateriaIdAndCursoIdAndPeriodoAcademicoId(
                any(), any(), any(), any())).thenReturn(false);
    }

    @Test
    void rechazaLaAsignacionQueSuperaElMaximoSemanal() {
        materiaDe(MATEMATICAS, 6);
        cargaActualDe(26); // 26 + 6 = 32 > 30

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.asignarMateria(peticion()));

        assertEquals("RB-09", error.getCodigoRegla());
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void aceptaLaAsignacionQueDejaAlDocenteJustoEnElMaximo() {
        // El limite es inclusivo: 30 horas es carga admisible, 31 no.
        materiaDe(MATEMATICAS, 4);
        cargaActualDe(26);
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.asignarMateria(peticion());

        verify(asignacionRepository).save(any());
    }

    @Test
    void aceptaLaPrimeraAsignacionDeUnDocenteSinCarga() {
        materiaDe(MATEMATICAS, 5);
        cargaActualDe();
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.asignarMateria(peticion());

        verify(asignacionRepository).save(any());
    }

    @Test
    void siguePrevaleciendoElAreaDeFormacionSobreElComputoDeHoras() {
        // RB-16 se comprueba antes: no tiene sentido discutir horas de una
        // materia que el docente no puede dictar.
        MateriaJpaEntity materia = materiaDe(MATEMATICAS, 2);
        materia.setArea("ARTES");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.asignarMateria(peticion()));

        assertEquals("RB-16", error.getCodigoRegla());
    }

    // ---------- utilidades ----------

    private MateriaJpaEntity materiaDe(Long id, int horas) {
        MateriaJpaEntity materia = new MateriaJpaEntity();
        materia.setId(id);
        materia.setArea(AREA);
        materia.setIntensidadHorariaSemanal(horas);
        lenient().when(materiaRepository.findById(id)).thenReturn(Optional.of(materia));
        return materia;
    }

    /** Deja al docente con una carga previa equivalente a {@code horas} totales. */
    private void cargaActualDe(int... horas) {
        List<AsignacionDocenteJpaEntity> asignaciones = new java.util.ArrayList<>();
        long siguienteMateriaId = 100L;
        for (int h : horas) {
            AsignacionDocenteJpaEntity a = new AsignacionDocenteJpaEntity();
            a.setDocenteId(DOCENTE);
            a.setMateriaId(siguienteMateriaId);
            a.setCursoId(CURSO);
            a.setPeriodoAcademicoId(PERIODO);
            asignaciones.add(a);
            materiaDe(siguienteMateriaId, h);
            siguienteMateriaId++;
        }
        lenient().when(asignacionRepository.findByDocenteIdAndPeriodoAcademicoId(DOCENTE, PERIODO))
                .thenReturn(asignaciones);
    }

    private static AsignarMateriaRequest peticion() {
        return new AsignarMateriaRequest(DOCENTE, MATEMATICAS, CURSO, PERIODO);
    }

    private static DocenteJpaEntity docente() {
        DocenteJpaEntity e = new DocenteJpaEntity();
        e.setId(DOCENTE);
        e.setDocumento("1010");
        e.setNombres("Ana");
        e.setApellidos("Ruiz");
        e.setCorreo("ana.ruiz@colegio.edu.co");
        e.getAreasFormacion().add(AREA);
        return e;
    }
}
