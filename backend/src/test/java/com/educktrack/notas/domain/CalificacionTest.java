package com.educktrack.notas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        ReglaNegocioException bajo = assertThrows(ReglaNegocioException.class,
                () -> new Calificacion(new BigDecimal("0.5")));
        ReglaNegocioException alto = assertThrows(ReglaNegocioException.class,
                () -> new Calificacion(new BigDecimal("5.5")));
        assertEquals("RB-03", bajo.getCodigoRegla());
        assertEquals("RB-03", alto.getCodigoRegla());
    }

    @Test
    void aceptaNotasEnEscalaYEvaluaAprobacion() {
        assertTrue(new Calificacion(new BigDecimal("3.0")).esAprobatoria());
        assertTrue(new Calificacion(new BigDecimal("5.0")).esAprobatoria());
        assertFalse(new Calificacion(new BigDecimal("2.9")).esAprobatoria());
        assertTrue(new Calificacion(new BigDecimal("2.9")).esBajoRendimiento());
    }

    @Test
    void losLimitesDeLaEscalaSonInclusivos() {
        // 1.0 y 5.0 son notas validas, no los primeros valores rechazados.
        assertEquals(new BigDecimal("1.00"), new Calificacion(new BigDecimal("1.0")).getValor());
        assertEquals(new BigDecimal("5.00"), new Calificacion(new BigDecimal("5.0")).getValor());
    }

    @Test
    void tratatIgualLaMismaNotaEscritaConDistintosDecimales() {
        // 3, 3.0 y 3.00 son el mismo numero. Con equals de BigDecimal no lo
        // serian (compara tambien la escala), de ahi que el dominio normalice
        // a dos decimales y compare con compareTo.
        assertEquals(new Calificacion(new BigDecimal("3")).getValor(),
                new Calificacion(new BigDecimal("3.00")).getValor());
        assertTrue(new Calificacion(new BigDecimal("3")).esAprobatoria());
    }

    @Test
    void laFronteraDeAprobacionEsExactaYNoDependeDelRedondeoBinario() {
        // El motivo del cambio a BigDecimal (Fase 8): con double, valores como
        // 2.9 o 3.0 no se representan exactamente, y RB-12 decide la aprobacion
        // comparando justo contra 3.0. Aqui la frontera es exacta por
        // construccion: 2.99 reprueba y 3.00 aprueba, sin margen de duda.
        assertFalse(new Calificacion(new BigDecimal("2.99")).esAprobatoria());
        assertTrue(new Calificacion(new BigDecimal("3.00")).esAprobatoria());
    }

    @Test
    void redondeaALaEscalaInstitucionalDeDosDecimales() {
        // La nota se guarda como DECIMAL(3,2): un tercer decimal se redondea al
        // registrarla, no al mostrarla, de modo que lo guardado y lo mostrado
        // no puedan discrepar.
        assertEquals(new BigDecimal("3.46"), new Calificacion(new BigDecimal("3.455")).getValor());
    }
}
