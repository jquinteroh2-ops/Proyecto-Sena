package com.educktrack.docentes.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del dominio Docente: al menos un area (HU-08) y RB-16 (dictar por area).
 */
class DocenteTest {

    @Test
    void exigeAlMenosUnAreaDeFormacion() {
        assertThrows(ReglaNegocioException.class,
                () -> Docente.registrar("123", "Ana", "Perez", "a@x.co", "1", Set.of()));
    }

    @Test
    void validaSiPuedeDictarSegunSuArea() {
        Docente docente = Docente.registrar("123", "Ana", "Perez", "a@x.co", "1",
                Set.of("Matematicas", "Fisica"));

        assertTrue(docente.puedeDictarArea("Matematicas"));
        assertFalse(docente.puedeDictarArea("Humanidades"));
    }
}
