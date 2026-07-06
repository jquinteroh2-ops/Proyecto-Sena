package com.educktrack.cursos.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la regla RB-17: un curso no puede matricular por encima del cupo.
 */
class CursoTest {

    private Curso cursoConCupo(int cupo) {
        return Curso.registrar("6A", 6, NivelEducativo.BASICA_SECUNDARIA, Jornada.MANANA, cupo, 1L);
    }

    @Test
    void rechazaCupoMaximoInvalido() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> cursoConCupo(0));
        assertEquals("RB-17", ex.getCodigoRegla());
    }

    @Test
    void indicaCupoDisponibleCorrectamente() {
        Curso curso = cursoConCupo(2);
        assertTrue(curso.tieneCupoDisponible(1));
        assertFalse(curso.tieneCupoDisponible(2));
    }

    @Test
    void validarCupoFallaAlAlcanzarElMaximo() {
        Curso curso = cursoConCupo(30);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> curso.validarCupo(30));
        assertEquals("RB-17", ex.getCodigoRegla());
    }
}
