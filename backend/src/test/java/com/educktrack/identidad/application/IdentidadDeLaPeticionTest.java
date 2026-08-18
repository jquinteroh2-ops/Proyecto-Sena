package com.educktrack.identidad.application;

import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la memorizacion de identidad por peticion (Fase 8, RNF-03).
 *
 * <p>Lo que se comprueba no es que sea rapido, sino las dos condiciones que
 * hacen que memorizar sea correcto: que dentro de una peticion la cuenta se lea
 * una sola vez, y que <strong>ninguna identidad sobreviva a la limpieza</strong>.
 * Lo segundo es lo importante: el servidor reutiliza hilos entre peticiones, y
 * una identidad que sobrevive es un fallo de aislamiento, no de rendimiento.</p>
 */
@ExtendWith(MockitoExtension.class)
class IdentidadDeLaPeticionTest {

    private static final String CORREO = "docente@colegio.edu.co";
    private static final String OTRO_CORREO = "rector@colegio.edu.co";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private VinculoAcudienteRepository vinculoRepository;
    @Mock private AsignacionDocenteRepository asignacionRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private MatriculaRepository matriculaRepository;

    @InjectMocks private ContextoUsuario contexto;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        IdentidadDeLaPeticion.limpiar();
    }

    @Test
    void leeLaCuentaUnaSolaVezAunqueSeConsulteVariasVeces() {
        autenticar(CORREO);
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuario(7L, NombreRol.DOCENTE)));

        contexto.usuarioActual();
        contexto.usuarioIdActual();
        contexto.rolesActuales();
        contexto.tieneVisionInstitucional();

        // Antes de la Fase 8 eran cuatro consultas; dentro de una peticion la
        // respuesta no puede cambiar, de modo que memorizarla es exacto.
        verify(usuarioRepository, times(1)).findByCorreoInstitucional(CORREO);
    }

    @Test
    void vuelveAConsultarDespuesDeLimpiar() {
        autenticar(CORREO);
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuario(7L, NombreRol.DOCENTE)));

        contexto.usuarioActual();
        IdentidadDeLaPeticion.limpiar(); // lo que hace el filtro al terminar
        contexto.usuarioActual();

        verify(usuarioRepository, times(2)).findByCorreoInstitucional(CORREO);
    }

    @Test
    void noReutilizaLaIdentidadMemorizadaParaOtroCorreo() {
        // La red de seguridad: si el filtro fallara y el hilo llegara sucio a la
        // peticion siguiente, la identidad memorizada NO debe servirle a otra
        // cuenta. Sin esta comprobacion, un fallo de limpieza se manifestaria
        // como una peticion respondida con los datos de otra persona.
        autenticar(CORREO);
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuario(7L, NombreRol.DOCENTE)));
        assertEquals(7L, contexto.usuarioIdActual());

        autenticar(OTRO_CORREO);
        when(usuarioRepository.findByCorreoInstitucional(OTRO_CORREO))
                .thenReturn(Optional.of(usuario(99L, NombreRol.RECTOR)));

        assertEquals(99L, contexto.usuarioIdActual());
    }

    @Test
    void memorizaElPerfilDeDocenteDeLaCuenta() {
        autenticar(CORREO);
        when(usuarioRepository.findByCorreoInstitucional(CORREO))
                .thenReturn(Optional.of(usuario(7L, NombreRol.DOCENTE)));
        when(docenteRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());

        contexto.docenteIdActual();
        contexto.docenteIdActual();

        verify(docenteRepository, times(1)).findByUsuarioId(7L);
    }

    // ---------- utilidades ----------

    private static void autenticar(String correo) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null, java.util.List.of()));
    }

    private static UsuarioJpaEntity usuario(Long id, NombreRol rol) {
        UsuarioJpaEntity u = new UsuarioJpaEntity();
        u.setId(id);
        u.setCorreoInstitucional(CORREO);
        RolJpaEntity r = new RolJpaEntity();
        r.setNombre(rol);
        Set<RolJpaEntity> roles = new HashSet<>();
        roles.add(r);
        u.setRoles(roles);
        return u;
    }
}
