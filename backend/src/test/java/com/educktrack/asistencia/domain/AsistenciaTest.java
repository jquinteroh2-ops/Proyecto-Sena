package com.educktrack.asistencia.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del dominio de asistencia: RB-06 (ventana de edicion de 48h).
 */
class AsistenciaTest {

    @Test
    void permiteEditarDentroDeLas48Horas() {
        Asistencia a = new Asistencia(EstadoAsistencia.AUSENTE, false, null,
                LocalDateTime.now().minusHours(10));
        assertTrue(a.puedeModificarse(LocalDateTime.now()));
        a.cambiarEstado(EstadoAsistencia.PRESENTE, LocalDateTime.now());
        assertEquals(EstadoAsistencia.PRESENTE, a.getEstado());
    }

    @Test
    void rechazaEdicionPasadas48Horas() {
        Asistencia a = new Asistencia(EstadoAsistencia.AUSENTE, false, null,
                LocalDateTime.now().minusHours(49));
        assertFalse(a.puedeModificarse(LocalDateTime.now()));
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> a.cambiarEstado(EstadoAsistencia.PRESENTE, LocalDateTime.now()));
        assertEquals("RB-06", ex.getCodigoRegla());
    }

    @Test
    void justificarMarcaLaInasistencia() {
        Asistencia a = new Asistencia(EstadoAsistencia.AUSENTE, false, null, LocalDateTime.now());
        a.justificar("Cita medica");
        assertTrue(a.isJustificada());
        assertEquals("Cita medica", a.getMotivoJustificacion());
    }
}
