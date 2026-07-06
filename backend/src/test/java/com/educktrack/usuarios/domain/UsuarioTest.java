package com.educktrack.usuarios.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la regla RB-14: una cuenta no puede ser Estudiante y Docente a la vez.
 */
class UsuarioTest {

    @Test
    void rechazaCrearUsuarioConRolesEstudianteYDocente() {
        Set<NombreRol> roles = EnumSet.of(NombreRol.ESTUDIANTE, NombreRol.DOCENTE);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> Usuario.nueva("Ana Perez", "ana@colegio.edu.co", "hash", roles));

        assertEquals("RB-14", ex.getCodigoRegla());
    }

    @Test
    void rechazaAsignarDocenteAUnEstudiante() {
        Usuario usuario = Usuario.nueva("Ana Perez", "ana@colegio.edu.co", "hash",
                EnumSet.of(NombreRol.ESTUDIANTE));

        assertThrows(ReglaNegocioException.class, () -> usuario.asignarRol(NombreRol.DOCENTE));
    }

    @Test
    void permiteCombinacionesValidasDeRoles() {
        Usuario usuario = Usuario.nueva("Luis Coordinador", "luis@colegio.edu.co", "hash",
                EnumSet.of(NombreRol.COORDINADOR_ACADEMICO, NombreRol.DOCENTE));

        assertTrue(usuario.getRoles().contains(NombreRol.DOCENTE));
        assertTrue(usuario.isActivo());
        assertTrue(usuario.isDebeCambiarPassword());
    }
}
