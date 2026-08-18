package com.educktrack.cursos.infrastructure.persistence;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.cursos.domain.NivelEducativo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de la consulta con bloqueo que sostiene RB-17 (cupo maximo de curso).
 *
 * <p>El cupo se comprueba contando matriculas activas y despues se inserta una
 * fila; entre las dos cosas el recuento puede quedar obsoleto. La Fase 3 cerro
 * RB-01 y RB-05 con indices unicos, pero "no mas de N filas" no es una regla de
 * unicidad y no se puede expresar asi: hace falta serializar sobre la fila del
 * curso.</p>
 *
 * <p>Esta prueba no puede demostrar la exclusion mutua sin concurrencia real
 * contra MySQL (sigue pendiente Testcontainers). Lo que sí comprueba, y es donde
 * esta el riesgo practico, es que la consulta con {@code @Lock} existe, es JPQL
 * valido y devuelve el curso: un fallo ahi no rompe la compilacion, solo aparece
 * al matricular.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ConsultaDeCupoTest {

    @Autowired private CursoRepository cursoRepository;

    @Test
    void devuelveElCursoBloqueandoSuFila() {
        Long cursoId = cursoRepository.save(curso("6-A", 20)).getId();

        Optional<CursoJpaEntity> curso = cursoRepository.findByIdParaMatricular(cursoId);

        assertTrue(curso.isPresent());
        assertEquals(20, curso.get().getCupoMaximo());
    }

    @Test
    void noDevuelveNadaSiElCursoNoExiste() {
        assertTrue(cursoRepository.findByIdParaMatricular(9999L).isEmpty());
    }

    private static CursoJpaEntity curso(String nombre, int cupoMaximo) {
        CursoJpaEntity e = new CursoJpaEntity();
        e.setNombre(nombre);
        e.setGrado(6);
        e.setNivel(NivelEducativo.BASICA_SECUNDARIA);
        e.setJornada(Jornada.MANANA);
        e.setCupoMaximo(cupoMaximo);
        e.setPeriodoAcademicoId(1L);
        return e;
    }
}
