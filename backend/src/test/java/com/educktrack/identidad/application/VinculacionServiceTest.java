package com.educktrack.identidad.application;

import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.domain.Parentesco;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la vinculacion entre cuentas y perfiles academicos (RF-11, RS-03)
 * y del refuerzo de RB-14 a nivel de datos.
 */
@ExtendWith(MockitoExtension.class)
class VinculacionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private VinculoAcudienteRepository vinculoRepository;
    @Mock private ContextoUsuario contextoUsuario;

    @InjectMocks private VinculacionService service;

    // ---------------------------------------------------------------------
    // Vinculacion de estudiante
    // ---------------------------------------------------------------------

    @Test
    void vinculaLaCuentaConElPerfilDelEstudiante() {
        EstudianteJpaEntity estudiante = estudiante(10L);
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(7L, NombreRol.ESTUDIANTE)));
        when(docenteRepository.existsByUsuarioId(7L)).thenReturn(false);
        when(estudianteRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());
        when(estudianteRepository.save(any())).thenReturn(estudiante);

        var dto = service.vincularEstudiante(10L, 7L);

        assertEquals(10L, dto.perfilId());
        assertEquals(7L, dto.usuarioId());
        assertEquals(7L, estudiante.getUsuarioId());
    }

    @Test
    void rechazaVincularUnaCuentaSinElRolEstudiante() {
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L)));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(7L, NombreRol.COORDINADOR_ACADEMICO)));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.vincularEstudiante(10L, 7L));

        assertEquals("RS-03", ex.getCodigoRegla());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaVincularComoEstudianteUnaCuentaQueYaEsDocente() {
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L)));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(7L, NombreRol.ESTUDIANTE)));
        when(docenteRepository.existsByUsuarioId(7L)).thenReturn(true);

        // RB-14 reforzada a nivel de datos, no solo sobre los roles.
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.vincularEstudiante(10L, 7L));

        assertEquals("RB-14", ex.getCodigoRegla());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaVincularUnEstudianteQueYaTieneOtraCuenta() {
        EstudianteJpaEntity estudiante = estudiante(10L);
        estudiante.setUsuarioId(99L);
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(7L, NombreRol.ESTUDIANTE)));

        assertThrows(ReglaNegocioException.class, () -> service.vincularEstudiante(10L, 7L));
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaVincularUnaCuentaDesactivada() {
        UsuarioJpaEntity desactivado = usuario(7L, NombreRol.ESTUDIANTE);
        desactivado.setActivo(false);
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante(10L)));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(desactivado));

        // HU-02: una cuenta desactivada no habilita acceso a datos academicos.
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.vincularEstudiante(10L, 7L));

        assertEquals("RF-03", ex.getCodigoRegla());
    }

    // ---------------------------------------------------------------------
    // Vinculacion de docente
    // ---------------------------------------------------------------------

    @Test
    void rechazaVincularComoDocenteUnaCuentaQueYaEsEstudiante() {
        when(docenteRepository.findById(55L)).thenReturn(Optional.of(docente(55L)));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(7L, NombreRol.DOCENTE)));
        when(estudianteRepository.existsByUsuarioId(7L)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.vincularDocente(55L, 7L));

        assertEquals("RB-14", ex.getCodigoRegla());
        verify(docenteRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Vinculacion de acudiente (RF-11)
    // ---------------------------------------------------------------------

    @Test
    void vinculaAlAcudienteConElEstudiante() {
        when(estudianteRepository.existsById(10L)).thenReturn(true);
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario(20L, NombreRol.PADRE_FAMILIA)));
        when(vinculoRepository.existsByUsuarioIdAndEstudianteId(20L, 10L)).thenReturn(false);
        when(vinculoRepository.save(any())).thenAnswer(inv -> {
            VinculoAcudienteJpaEntity v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        var dto = service.vincularAcudiente(10L, 20L, Parentesco.MADRE);

        assertEquals(10L, dto.estudianteId());
        assertEquals(20L, dto.usuarioId());
        assertEquals(Parentesco.MADRE, dto.parentesco());
    }

    @Test
    void rechazaVincularComoAcudienteUnaCuentaSinElRolPadreFamilia() {
        when(estudianteRepository.existsById(10L)).thenReturn(true);
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario(20L, NombreRol.DOCENTE)));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.vincularAcudiente(10L, 20L, Parentesco.PADRE));

        assertEquals("RS-03", ex.getCodigoRegla());
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    void rechazaDuplicarElVinculoDeUnAcudienteConElMismoEstudiante() {
        when(estudianteRepository.existsById(10L)).thenReturn(true);
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario(20L, NombreRol.PADRE_FAMILIA)));
        when(vinculoRepository.existsByUsuarioIdAndEstudianteId(20L, 10L)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.vincularAcudiente(10L, 20L, Parentesco.PADRE));

        assertEquals("RF-11", ex.getCodigoRegla());
        verify(vinculoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------

    private static UsuarioJpaEntity usuario(Long id, NombreRol rol) {
        UsuarioJpaEntity u = new UsuarioJpaEntity();
        u.setId(id);
        u.setNombre("Cuenta de prueba");
        u.setCorreoInstitucional("cuenta" + id + "@colegio.edu.co");
        u.setActivo(true);
        u.setRoles(Set.of(new RolJpaEntity(rol, rol.name())));
        return u;
    }

    private static EstudianteJpaEntity estudiante(Long id) {
        EstudianteJpaEntity e = new EstudianteJpaEntity();
        e.setId(id);
        e.setNombres("Valentina");
        e.setApellidos("Rios");
        return e;
    }

    private static DocenteJpaEntity docente(Long id) {
        DocenteJpaEntity d = new DocenteJpaEntity();
        d.setId(id);
        d.setNombres("Carlos");
        d.setApellidos("Mendoza");
        return d;
    }
}
