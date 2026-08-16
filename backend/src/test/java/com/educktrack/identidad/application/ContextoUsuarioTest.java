package com.educktrack.identidad.application;

import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la resolucion de identidad y del control de acceso a nivel de dato
 * (RNF-07, RB-02, RB-08). Cubren el escenario central de las Fases 1 y 2: un
 * identificador recibido por la API no puede dar acceso a informacion ajena, y
 * el alcance del docente termina donde termina su carga academica.
 */
@ExtendWith(MockitoExtension.class)
class ContextoUsuarioTest {

    private static final String CORREO = "usuario@colegio.edu.co";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private VinculoAcudienteRepository vinculoRepository;
    @Mock private AsignacionDocenteRepository asignacionRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private MatriculaRepository matriculaRepository;

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
        when(docenteRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());

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
        when(docenteRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());

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
    // Vision institucional (RS-03)
    // ---------------------------------------------------------------------

    @Test
    void laCoordinacionConservaVisibilidadInstitucional() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.COORDINADOR_ACADEMICO)));

        assertTrue(contexto.tieneVisionInstitucional());
        assertTrue(contexto.puedeVerEstudiante(999L));
        assertEquals(999L, contexto.resolverEstudianteId(999L));
    }

    @Test
    void quienNoEsEstudianteDebeIndicarSobreQueEstudianteConsulta() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.DOCENTE)));
        when(estudianteRepository.findByUsuarioId(3L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> contexto.resolverEstudianteId(null));
    }

    // ---------------------------------------------------------------------
    // Alcance del docente (Fase 2: RNF-07, RB-02)
    // ---------------------------------------------------------------------

    @Test
    void elDocenteYaNoTieneVisibilidadInstitucional() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.DOCENTE)));

        // Cambio central de la Fase 2: el docente sale de ROLES_VISION_INSTITUCIONAL.
        assertFalse(contexto.tieneVisionInstitucional());
    }

    @Test
    void elDocenteAlcanzaALosEstudiantesActivosDeSusCursos() {
        autenticar();
        docenteAutenticado(3L, 55L);
        when(estudianteRepository.findByUsuarioId(3L)).thenReturn(Optional.empty());
        when(vinculoRepository.existsByUsuarioIdAndEstudianteId(3L, 10L)).thenReturn(false);
        when(asignacionRepository.findCursoIdsByDocenteId(55L)).thenReturn(List.of(100L));
        when(cursoRepository.findIdsByDirectorGrupoId(55L)).thenReturn(List.of());
        when(matriculaRepository.existsByEstudianteIdAndCursoIdInAndEstado(
                eq(10L), anyCollection(), eq(EstadoMatriculaCurso.ACTIVA))).thenReturn(true);

        assertTrue(contexto.puedeVerEstudiante(10L));
    }

    @Test
    void elDocenteNoAlcanzaAUnEstudianteDeUnCursoAjeno() {
        autenticar();
        docenteAutenticado(3L, 55L);
        when(estudianteRepository.findByUsuarioId(3L)).thenReturn(Optional.empty());
        when(vinculoRepository.existsByUsuarioIdAndEstudianteId(3L, 999L)).thenReturn(false);
        when(asignacionRepository.findCursoIdsByDocenteId(55L)).thenReturn(List.of(100L));
        when(cursoRepository.findIdsByDirectorGrupoId(55L)).thenReturn(List.of());
        when(matriculaRepository.existsByEstudianteIdAndCursoIdInAndEstado(
                eq(999L), anyCollection(), eq(EstadoMatriculaCurso.ACTIVA))).thenReturn(false);

        // Un docente de septimo no puede leer el expediente de un estudiante de once.
        assertThrows(AccessDeniedException.class, () -> contexto.exigirAccesoEstudiante(999L));
    }

    @Test
    void elDirectorDeGrupoAlcanzaSuCursoAunqueNoDicteMateriaEnEl() {
        autenticar();
        docenteAutenticado(3L, 55L);
        when(asignacionRepository.findCursoIdsByDocenteId(55L)).thenReturn(List.of());
        when(cursoRepository.findIdsByDirectorGrupoId(55L)).thenReturn(List.of(100L));

        // RB-02: la direccion de grupo es la segunda via de alcance.
        assertTrue(contexto.puedeVerCurso(100L));
    }

    @Test
    void elDocenteSoloGestionaLasMateriasQueTieneAsignadas() {
        autenticar();
        docenteAutenticado(3L, 55L);
        when(asignacionRepository.existsByDocenteIdAndCursoIdAndMateriaId(55L, 100L, 7L)).thenReturn(true);
        when(asignacionRepository.existsByDocenteIdAndCursoIdAndMateriaId(55L, 100L, 8L)).thenReturn(false);

        assertTrue(contexto.puedeGestionarMateria(100L, 7L));
        // RNF-07: no puede calificar ni pasar lista de una materia que no dicta.
        assertThrows(AccessDeniedException.class, () -> contexto.exigirGestionMateria(100L, 8L));
    }

    @Test
    void elDirectorDeGrupoNoPuedeCalificarMateriasQueNoDicta() {
        autenticar();
        docenteAutenticado(3L, 55L);
        when(asignacionRepository.existsByDocenteIdAndCursoIdAndMateriaId(55L, 100L, 8L)).thenReturn(false);

        // RB-02 da visibilidad sobre el curso, no potestad para poner notas.
        assertFalse(contexto.puedeGestionarMateria(100L, 8L));
    }

    // ---------------------------------------------------------------------
    // Cuenta propia (RF-54)
    // ---------------------------------------------------------------------

    @Test
    void laBandejaDeOtraCuentaQuedaFueraDeAlcance() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(7L, NombreRol.ESTUDIANTE)));

        contexto.exigirCuentaPropia(7L); // la propia no lanza
        assertThrows(AccessDeniedException.class, () -> contexto.exigirCuentaPropia(8L));
    }

    @Test
    void niSiquieraLaVisionInstitucionalAbreLaBandejaAjena() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(3L, NombreRol.RECTOR)));

        // Leer los mensajes de otro no es una funcion academica (RF-54).
        assertThrows(AccessDeniedException.class, () -> contexto.exigirCuentaPropia(8L));
    }

    // ---------------------------------------------------------------------
    // Perfiles y sesiones invalidas
    // ---------------------------------------------------------------------

    @Test
    void resuelveElDocenteAsociadoALaCuenta() {
        autenticar();
        docenteAutenticado(3L, 55L);

        assertEquals(55L, contexto.exigirDocenteId());
    }

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

    @Test
    void unaCuentaQueNoEsDocenteNoGanaAlcancePorLaViaDeLaCargaAcademica() {
        autenticar();
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(20L, NombreRol.PADRE_FAMILIA)));
        when(docenteRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());

        assertTrue(contexto.cursosDelDocente().isEmpty());
    }

    // ---------------------------------------------------------------------

    private static void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CORREO, null, List.of()));
    }

    /** Deja la sesion resuelta como un docente con perfil asociado. */
    private void docenteAutenticado(Long usuarioId, Long docenteId) {
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuarioCon(usuarioId, NombreRol.DOCENTE)));
        when(docenteRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(docente(docenteId)));
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
