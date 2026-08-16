package com.educktrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la API REST de EduckTrack (RS-13: API REST bajo MVC).
 *
 * <p>La organizacion del codigo sigue arquitectura hexagonal por paquetes de
 * modulo (RNF-17): {@code com.educktrack.<modulo>.{domain, application, infrastructure}}.
 * El paquete {@code com.educktrack.shared} contiene configuracion transversal.</p>
 *
 * <p>{@code @EnableScheduling} habilita las alertas que dependen del calendario
 * y no de una accion del usuario (RF-42 tareas por vencer, RF-55 fecha limite
 * de cierre). Ver {@code AlertasProgramadas}.</p>
 */
@SpringBootApplication
@EnableScheduling
public class EduckTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduckTrackApplication.class, args);
    }
}
