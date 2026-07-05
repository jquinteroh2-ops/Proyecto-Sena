package com.educktrack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de humo minima de la Fase 0: verifica que el harness de pruebas
 * (RNF-19) esta operativo. Las pruebas de reglas de negocio criticas
 * (RB-03, RB-04, RB-07, RB-15, RB-18) se agregan en las fases correspondientes.
 */
class SmokeTest {

    @Test
    void elHarnessDePruebasFunciona() {
        assertTrue(true, "El entorno de pruebas de EduckTrack esta operativo");
    }
}
