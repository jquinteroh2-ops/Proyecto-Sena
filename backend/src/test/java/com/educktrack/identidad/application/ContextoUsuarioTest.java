package com.educktrack.identidad.application;

import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la resolucion de identidad y del control de acceso a nivel de dato
 * (RNF-07, RB-08). Cubren el escenario central de la Fase 1: un identificador
 * recibido por la API no puede dar acceso a informacion ajena.
 */
@ExtendWith(MockitoExtension.class)
class ContextoUsuarioTest {

    private static final String CORREO = "usuario@colegio.edu.co";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private VinculoAcudienteRepository vinculoRepository;

    @InjectMocks private ContextoUsuario contexto;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------
    // Estudiante: solo su propia informacion (RNF-07)
    // ---------------------------------------------------------------------

    @Test
    void elEstudianteResuelveSuPropioIdIgnorandoElParametroRecibido() {
        autenticar();
        UsuarioJpaEntity usuario = usuarioCon(7L, NombreRol.ESTUDIANTE);
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario));
        when(estudianteRepository.findByUsuarioId(7L)).thenReturn(Optional.of(estudiante(10L)));

        // Aunque la peticion pida el estudiante 999, se resuelve el propio (10).
        assertEquals(10L, contexto.resolverEstudianteId(999L));
        assertEquals(10L, contexto.resolverEstudianteId(null));
    }

    @Test
    void elEstudianteNoPuedeVerLaInformacionDeOtroEstudiante() {
        autenticar();
        UsuarioJpaEntity usuario = usuarioCon(7L, NombreRol.ESTUDIANTE);
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario));
        when(estudianteRepository.findByUsuarioId(7L)).thenReturn(Optional.of(estudiante(10L)));
        when(vinculoRepository.existsByUsuarioIdAndEstudianteId(7L, 999L)).thenReturn(false);

        assertFalse(contexto.puedeVerEstudiante(999L));
        assertTrue(contexto.puedeVerEstudiante(10L));
    }

    // ---------------------------------------------------------------------
    // Acudiente: solo los estudiantes vinculados (RB-08)
    // ---------------------------------------------------------------------

    @Test
    void elAcudienteSoloAccedeALosEstudiantesVinculadosASuCuenta() {
        autenticar();
        UsuarioJpaEntity usuario = usuarioCon(20L, NombreRol.PADRE_FAMILIA);
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario));
        when(estudianteRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());
        when(vinculoRepository.existsByUsuarioIdAndEstudianteId(20L, 10L)).thenReturn(true);

        assertEquals(10L, contexto.resolverEstudianteId(10L));
    }

    @Test
    void elAcudienteEsRechazadoAlPedirUnEstudianteNoVinculado() {
        autenticar();
        UsuarioJpaEntity usuario = usuarioCon(20L, NombreRol.PADRE_FAMILIA);
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario));
        when(estudianteRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());
        when(vinculoRepository.existsByUsuarioIdAndEstudianteId(20L, 999L)).thenReturn(false);

        // RB-08: el vinculo formal es la unica via de visibilidad.
        assertThrows(AccessDeniedException.class, () -> contexto.resolverEstudianteId(999L));
    }

    @Test
    void elAcudienteListaLosEstudiantesQueTutela() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(20L, NombreRol.PADRE_FAMILIA)));
        when(vinculoRepository.findByUsuarioId(20L)).thenReturn(List.of(vinculo(10L), vinculo(11L)));

        assertEquals(List.of(10L, 11L), contexto.estudiantesTutelados());
    }

    // ---------------------------------------------------------------------
    // Personal academico (RS-03)
    // ---------------------------------------------------------------------

    @Test
    void elPersonalAcademicoConservaVisibilidadInstitucional() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.COORDINADOR_ACADEMICO)));

        assertTrue(contexto.esPersonalAcademico());
        assertTrue(contexto.puedeVerEstudiante(999L));
        assertEquals(999L, contexto.resolverEstudianteId(999L));
    }

    @Test
    void elPersonalAcademicoDebeIndicarSobreQueEstudianteConsulta() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.DOCENTE)));

        assertThrows(IllegalArgumentException.class, () -> contexto.resolverEstudianteId(null));
    }

    @Test
    void resuelveElDocenteAsociadoALaCuenta() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.DOCENTE)));
        when(docenteRepository.findByUsuarioId(3L)).thenReturn(Optional.of(docente(55L)));

        assertEquals(55L, contexto.exigirDocenteId());
    }

    // ---------------------------------------------------------------------
    // Sesiones invalidas
    // ---------------------------------------------------------------------

    @Test
    void rechazaLaPeticionSinUsuarioAutenticado() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () -> contexto.correoActual());
    }

    @Test
    void rechazaLaSesionCuyaCuentaYaNoExiste() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> contexto.usuarioActual());
    }

    @Test
    void unaCuentaSinPerfilDeDocenteNoPuedeOperarComoDocente() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.DOCENTE)));
        when(docenteRepository.findByUsuarioId(3L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> contexto.exigirDocenteId());
    }

    // ---------------------------------------------------------------------

    private static void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CORREO, null, List.of()));
    }

    private static UsuarioJpaEntity usuarioCon(Long id, NombreRol rol) {
        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(id);
        usuario.setNombre("Usuario de prueba");
        usuario.setCorreoInstitucional(CORREO);
        usuario.setRoles(Set.of(new RolJpaEntity(rol, rol.name())));
        return usuario;
    }

    private static EstudianteJpaEntity estudiante(Long id) {
        EstudianteJpaEntity e = new EstudianteJpaEntity();
        e.setId(id);
        return e;
    }

    private static DocenteJpaEntity docente(Long id) {
        DocenteJpaEntity d = new DocenteJpaEntity();
        d.setId(id);
        return d;
    }

    private static VinculoAcudienteJpaEntity vinculo(Long estudianteId) {
        VinculoAcudienteJpaEntity v = new VinculoAcudienteJpaEntity();
        v.setEstudianteId(estudianteId);
        return v;
    }
}
