package com.educktrack.notificaciones.application;

import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la traduccion de sujetos academicos a cuentas de usuario (RS-08,
 * RB-08).
 *
 * <p>El caso que mas importa es el perfil sin cuenta: el vinculo usuario-perfil
 * de V9 es opcional a proposito, y un {@code usuario_id} nulo colandose en la
 * lista de destinatarios rompe el envio de todos los demas.</p>
 */
@ExtendWith(MockitoExtension.class)
class DestinatariosServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private VinculoAcudienteRepository vinculoRepository;
    @Mock private AsignacionDocenteRepository asignacionRepository;
    @Mock private MatriculaRepository matriculaRepository;

    @InjectMocks private DestinatariosService service;

    @Test
    void incluyeLaCuentaDelEstudianteYLaDeSusAcudientes() {
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, 100L)));
        when(vinculoRepository.findByEstudianteId(10L))
                .thenReturn(List.of(vinculo(200L), vinculo(201L)));

        assertEquals(Set.of(100L, 200L, 201L), service.delEstudianteYSusAcudientes(10L));
    }

    @Test
    void unEstudianteSinCuentaNoImpideAvisarASusAcudientes() {
        // RF-06 y RF-09 permiten registrar y matricular antes de crear la cuenta.
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, null)));
        when(vinculoRepository.findByEstudianteId(10L)).thenReturn(List.of(vinculo(200L)));

        Set<Long> resultado = service.delEstudianteYSusAcudientes(10L);

        assertEquals(Set.of(200L), resultado);
        assertTrue(resultado.stream().allMatch(java.util.Objects::nonNull));
    }

    @Test
    void unEstudianteSinCuentaNiAcudientesNoDevuelveDestinatarios() {
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L, null)));
        when(vinculoRepository.findByEstudianteId(10L)).thenReturn(List.of());

        assertTrue(service.delEstudianteYSusAcudientes(10L).isEmpty());
    }

    @Test
    void elAvisoDeRetiroExcluyeLaCuentaDelEstudiante() {
        when(vinculoRepository.findByEstudianteId(10L)).thenReturn(List.of(vinculo(200L)));

        // HU-07 dirige el aviso al acudiente: no se consulta la cuenta del estudiante.
        assertEquals(Set.of(200L), service.deLosAcudientes(10L));
    }

    @Test
    void resuelveLasCuentasDeLosDocentesDeUnCurso() {
        when(asignacionRepository.findDocenteIdsByCursoId(5L)).thenReturn(List.of(55L, 66L));
        when(docenteRepository.findAllById(List.of(55L, 66L)))
                .thenReturn(List.of(docente(55L, 300L), docente(66L, 301L)));

        assertEquals(Set.of(300L, 301L), service.deLosDocentesDelCurso(5L));
    }

    @Test
    void unDocenteSinCuentaSeDescartaSinRomperElResto() {
        when(asignacionRepository.findDocenteIdsByCursoId(5L)).thenReturn(List.of(55L, 66L));
        when(docenteRepository.findAllById(List.of(55L, 66L)))
                .thenReturn(List.of(docente(55L, null), docente(66L, 301L)));

        assertEquals(Set.of(301L), service.deLosDocentesDelCurso(5L));
    }

    @Test
    void unCursoSinDocentesAsignadosNoConsultaCuentas() {
        when(asignacionRepository.findDocenteIdsByCursoId(5L)).thenReturn(List.of());

        assertTrue(service.deLosDocentesDelCurso(5L).isEmpty());
    }

    // ---------------------------------------------------------------------

    private static EstudianteJpaEntity estudiante(Long id, Long usuarioId) {
        EstudianteJpaEntity e = new EstudianteJpaEntity();
        e.setId(id);
        e.setUsuarioId(usuarioId);
        return e;
    }

    private static DocenteJpaEntity docente(Long id, Long usuarioId) {
        DocenteJpaEntity d = new DocenteJpaEntity();
        d.setId(id);
        d.setUsuarioId(usuarioId);
        return d;
    }

    private static VinculoAcudienteJpaEntity vinculo(Long usuarioId) {
        VinculoAcudienteJpaEntity v = new VinculoAcudienteJpaEntity();
        v.setUsuarioId(usuarioId);
        return v;
    }
}
