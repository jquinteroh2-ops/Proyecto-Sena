package com.educktrack.notas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del dominio Calificacion: RB-03 (escala 1.0-5.0) y aprobatoria (RD-01).
 */
class CalificacionTest {

    @Test
    void rechazaNotaFueraDeEscala() {
        ReglaNegocioException bajo = assertThrows(ReglaNegocioException.class, () -> new Calificacion(0.5));
        ReglaNegocioException alto = assertThrows(ReglaNegocioException.class, () -> new Calificacion(5.5));
        assertEquals("RB-03", bajo.getCodigoRegla());
        assertEquals("RB-03", alto.getCodigoRegla());
    }

    @Test
    void aceptaNotasEnEscalaYEvaluaAprobacion() {
        assertTrue(new Calificacion(3.0).esAprobatoria());
        assertTrue(new Calificacion(5.0).esAprobatoria());
        assertFalse(new Calificacion(2.9).esAprobatoria());
        assertTrue(new Calificacion(2.9).esBajoRendimiento());
    }
}
