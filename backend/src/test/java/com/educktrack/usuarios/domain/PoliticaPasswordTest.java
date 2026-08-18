package com.educktrack.usuarios.domain;

import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de la politica minima de contrasena (HU-01, HU-04).
 *
 * <p>Existe como concepto de dominio porque la regla vivia solo en una
 * anotacion del DTO de registro, y HU-04 exige aplicarla tambien al recuperar
 * la cuenta. Una regla declarada en un unico punto de entrada no es una
 * politica: la siguiente via de fijar contrasena nace sin ella.</p>
 */
class PoliticaPasswordTest {

    @Test
    void aceptaUnaPasswordQueCumpleLaLongitudMinima() {
        assertDoesNotThrow(() -> PoliticaPassword.exigirCumplimiento("clave123"));
    }

    @Test
    void rechazaUnaPasswordDemasiadoCorta() {
        assertThrows(ReglaNegocioException.class, () -> PoliticaPassword.exigirCumplimiento("corta12"));
    }

    @Test
    void rechazaLaAusenciaDePassword() {
        assertThrows(ReglaNegocioException.class, () -> PoliticaPassword.exigirCumplimiento(null));
        assertThrows(ReglaNegocioException.class, () -> PoliticaPassword.exigirCumplimiento("        "));
    }
}
