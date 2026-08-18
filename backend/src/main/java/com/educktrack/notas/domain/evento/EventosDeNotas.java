package com.educktrack.notas.domain.evento;

import java.math.BigDecimal;

/**
 * Hechos del modulo de calificaciones que otros modulos pueden querer atender
 * (RS-08).
 *
 * <p>Los eventos viven en el modulo que los publica, no en el que los consume:
 * asi {@code notas} no necesita conocer al modulo de notificaciones, y quien
 * quiera reaccionar a un cierre de corte manana no obliga a tocar este codigo.
 * Es la direccion de dependencia que buscaba esta fase.</p>
 *
 * <p>Los eventos declaran <strong>lo que ocurrio</strong>, no lo que hay que
 * hacer al respecto: no llevan destinatarios ni textos de mensaje. Decidir a
 * quien se avisa y con que palabras es competencia de quien escucha.</p>
 */
public final class EventosDeNotas {

    private EventosDeNotas() {
    }

    /**
     * RB-13: se registro una calificacion por debajo de la nota aprobatoria.
     * El umbral lo evalua quien publica, porque la regla es de calificaciones.
     */
    public record NotaBajaRegistrada(
            Long estudianteId,
            Long materiaId,
            Long cursoId,
            Long periodoAcademicoId,
            BigDecimal valor) {
    }

    /**
     * RF-56 / RB-19: se cerro el corte de un curso. A partir de ese momento el
     * boletin puede generarse, que es justo cuando HU-21 pide avisar a
     * estudiantes y acudientes.
     */
    public record CorteCerrado(
            Long cursoId,
            Long periodoAcademicoId) {
    }
}
