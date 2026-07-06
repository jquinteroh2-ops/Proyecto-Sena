package com.educktrack.tareas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del dominio Tarea: HU-23 (fecha limite) y RB-10 (plazo de entrega).
 */
class TareaTest {

    private final LocalDate hoy = LocalDate.of(2026, 3, 1);

    @Test
    void rechazaFechaLimiteAnterior() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> new Tarea(hoy.minusDays(1), false, hoy));
        assertEquals("HU-23", ex.getCodigoRegla());
    }

    @Test
    void aplicaRB10SegunPlazoYAutorizacion() {
        Tarea tarea = new Tarea(hoy.plusDays(5), false, hoy);
        assertTrue(tarea.admiteEntrega(hoy.plusDays(5)));      // en plazo
        assertFalse(tarea.admiteEntrega(hoy.plusDays(6)));     // tarde, sin autorizacion
        assertThrows(ReglaNegocioException.class, () -> tarea.validarEntrega(hoy.plusDays(6)));
    }

    @Test
    void entregaTardiaAutorizadaSeAdmite() {
        Tarea tarea = new Tarea(hoy.plusDays(5), true, hoy);
        assertTrue(tarea.admiteEntrega(hoy.plusDays(10)));     // autorizada
    }
}
