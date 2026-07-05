package com.educktrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST de EduckTrack (RS-13: API REST bajo MVC).
 *
 * <p>La organizacion del codigo sigue arquitectura hexagonal por paquetes de
 * modulo (RNF-17): {@code com.educktrack.<modulo>.{domain, application, infrastructure}}.
 * El paquete {@code com.educktrack.shared} contiene configuracion transversal.</p>
 */
@SpringBootApplication
public class EduckTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduckTrackApplication.class, args);
    }
}
