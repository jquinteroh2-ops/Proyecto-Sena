package com.educktrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring se levanta completo.
 *
 * <p>Es una red de seguridad barata pero decisiva: la Fase 2 inyecto
 * {@code ContextoUsuario} en siete servicios de aplicacion y anadio consultas
 * derivadas nuevas. Un ciclo entre beans o un nombre de metodo derivado mal
 * escrito no rompe la compilacion ni las pruebas unitarias con dobles: solo
 * revienta al arrancar. Sin esta prueba, el primer sintoma seria un despliegue
 * que no levanta.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ContextoDeAplicacionTest {

    @Test
    void elContextoDeAplicacionSeLevantaConTodoElCableado() {
        // El propio arranque del contexto es la asercion.
    }
}
