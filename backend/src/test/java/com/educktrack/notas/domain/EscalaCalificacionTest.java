package com.educktrack.notas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del invariante de la escala de calificacion (RB-03, RF-59).
 *
 * <p>Desde la Fase 9 la escala la fija el Rector, de modo que puede llegar mal.
 * El invariante se valida al construirla y no al usarla: una escala imposible
 * haria que ninguna nota aprobase nunca, y ese fallo aparecería mucho despues,
 * calificando, en vez de al guardarla.</p>
 */
class EscalaCalificacionTest {

    @Test
    void aceptaLaEscalaDelEnunciado() {
        assertDoesNotThrow(() -> new EscalaCalificacion(
                new BigDecimal("1.00"), new BigDecimal("5.00"), new BigDecimal("3.00")));
    }

    @Test
    void aceptaUnaEscalaInstitucionalDistinta() {
        // RF-59 existe justamente para esto: otra institucion puede calificar
        // sobre 10 y aprobar desde 6.
        EscalaCalificacion sobreDiez = new EscalaCalificacion(
                new BigDecimal("0.00").add(BigDecimal.ONE), new BigDecimal("10.00"), new BigDecimal("6.00"));
        assertTrue(sobreDiez.esAprobatoria(new BigDecimal("6.00")));
        assertFalse(sobreDiez.esAprobatoria(new BigDecimal("5.99")));
        assertTrue(sobreDiez.contiene(new BigDecimal("10.00")));
    }

    @Test
    void rechazaLaEscalaConMinimaMayorOIgualQueLaMaxima() {
        assertThrows(ReglaNegocioException.class, () -> new EscalaCalificacion(
                new BigDecimal("5.00"), new BigDecimal("1.00"), new BigDecimal("3.00")));
        assertThrows(ReglaNegocioException.class, () -> new EscalaCalificacion(
                new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("5.00")));
    }

    @Test
    void rechazaLaEscalaEnLaQueNadiePodriaAprobar() {
        // Aprobatoria por encima del maximo: ninguna nota valida aprueba.
        assertThrows(ReglaNegocioException.class, () -> new EscalaCalificacion(
                new BigDecimal("1.00"), new BigDecimal("5.00"), new BigDecimal("5.01")));
    }

    @Test
    void rechazaLaEscalaEnLaQueTodosAprobarian() {
        // Aprobatoria igual o por debajo de la minima: ninguna nota reprueba, y
        // la regla de aprobacion (RB-12) dejaria de significar nada.
        assertThrows(ReglaNegocioException.class, () -> new EscalaCalificacion(
                new BigDecimal("1.00"), new BigDecimal("5.00"), new BigDecimal("1.00")));
    }

    @Test
    void laFronteraDeAprobacionEsInclusiva() {
        assertTrue(EscalaCalificacion.POR_DEFECTO.esAprobatoria(new BigDecimal("3.00")));
        assertFalse(EscalaCalificacion.POR_DEFECTO.esAprobatoria(new BigDecimal("2.99")));
    }
}
