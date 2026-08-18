package com.educktrack.seguridad.application;

import com.educktrack.seguridad.infrastructure.persistence.TokenRecuperacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Borra los enlaces de recuperacion que ya no sirven (RF-64, HU-04).
 *
 * <p>La tabla solo crecia: cada solicitud dejaba una fila y ninguna se
 * eliminaba nunca. No es un problema de espacio a corto plazo, pero un token
 * gastado o caducado no aporta nada pasadas unas horas, y guardar
 * indefinidamente credenciales muertas —aunque sean hashes— es guardar mas de
 * lo necesario.</p>
 *
 * <p><strong>No borra de inmediato.</strong> Se conservan unos dias porque un
 * token gastado que reaparece es justo lo que interesa poder investigar: si se
 * borrase al usarlo, un intento de reutilizacion no se distinguiria de un token
 * inventado.</p>
 *
 * <p>Comparte la limitacion de {@code AlertasProgramadas}: con varias
 * instancias correria en todas. Aqui es inofensivo —borrar dos veces lo mismo
 * no rompe nada— a diferencia de las alertas, que se duplicarian.</p>
 */
@Component
public class LimpiezaTokensRecuperacion {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaTokensRecuperacion.class);

    private final TokenRecuperacionRepository repository;
    private final int diasRetencion;

    public LimpiezaTokensRecuperacion(TokenRecuperacionRepository repository,
                                      @Value("${educktrack.seguridad.recuperacion.dias-retencion:7}")
                                      int diasRetencion) {
        this.repository = repository;
        this.diasRetencion = diasRetencion;
    }

    /** Una vez al dia, de madrugada, cuando no hay nadie usando el sistema. */
    @Scheduled(cron = "${educktrack.seguridad.recuperacion.cron-limpieza:0 30 3 * * *}")
    @Transactional
    public void eliminarCaducados() {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasRetencion);
        int borrados = repository.eliminarCaducadosAnterioresA(limite);
        if (borrados > 0) {
            log.info("Limpieza de enlaces de recuperacion: {} eliminados (caducados antes de {}).",
                    borrados, limite);
        }
    }
}
