package com.educktrack.horarios.domain;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del dominio de horarios: HU-11 (fin>inicio) y solapamiento (RB-18).
 */
class BloqueHorarioTest {

    @Test
    void rechazaHoraFinAnteriorAInicio() {
        assertThrows(ReglaNegocioException.class,
                () -> BloqueHorario.crear(DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(8, 0), Jornada.MANANA));
    }

    @Test
    void detectaSolapamientoElMismoDia() {
        BloqueHorario a = BloqueHorario.crear(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), Jornada.MANANA);
        BloqueHorario b = BloqueHorario.crear(DiaSemana.LUNES, LocalTime.of(8, 30), LocalTime.of(9, 30), Jornada.MANANA);
        BloqueHorario c = BloqueHorario.crear(DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0), Jornada.MANANA);
        BloqueHorario otroDia = BloqueHorario.crear(DiaSemana.MARTES, LocalTime.of(8, 0), LocalTime.of(9, 0), Jornada.MANANA);

        assertTrue(a.seSolapaCon(b));
        assertFalse(a.seSolapaCon(c)); // contiguos, no se solapan
        assertFalse(a.seSolapaCon(otroDia));
    }
}
