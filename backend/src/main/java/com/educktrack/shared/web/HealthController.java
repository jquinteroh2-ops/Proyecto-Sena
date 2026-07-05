package com.educktrack.shared.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Endpoint de verificacion de estado del servicio.
 *
 * <p>Sirve como comprobacion de vida basica del backend durante el desarrollo
 * y despliegue (usado por el healthcheck de Docker en fases posteriores).</p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /** Devuelve el estado del backend. No implementa un RF; es soporte operativo. */
    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "service", "educktrack-backend",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString());
    }
}
