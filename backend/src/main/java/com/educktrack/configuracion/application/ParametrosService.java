package com.educktrack.configuracion.application;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.configuracion.domain.ParametroInstitucional;
import com.educktrack.configuracion.infrastructure.persistence.ParametroJpaEntity;
import com.educktrack.configuracion.infrastructure.persistence.ParametroRepository;
import com.educktrack.notas.domain.EscalaCalificacion;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parametros institucionales (RF-59, RS-14): escala de calificacion (RB-03),
 * porcentaje minimo de asistencia (RB-04) y carga maxima del docente (RB-09).
 *
 * <p>Antes de la Fase 9 estos valores eran constantes de codigo o propiedades
 * del despliegue, de modo que cambiarlos exigia recompilar o reiniciar. RF-59
 * pide que los defina el Rector desde el sistema.</p>
 *
 * <h2>Por que hay cache</h2>
 *
 * <p>La escala se consulta al registrar <em>cada</em> nota y el minimo de
 * asistencia al calcular <em>cada</em> porcentaje. Sin memoria, la Fase 9
 * desharia parte de lo que consiguio la Fase 8: una consulta mas por operacion.
 * Los parametros cambian a lo sumo unas pocas veces al ano, asi que se leen una
 * vez y se refrescan al actualizarlos.</p>
 *
 * <p><strong>La cache es por instancia.</strong> Con escalado horizontal
 * (RNF-13) un cambio tardaria en verse en las demas, igual que el planificador
 * de la Fase 5 no esta preparado para varias instancias. Hoy el despliegue es
 * de una sola.</p>
 */
@Service
public class ParametrosService {

    private static final Logger log = LoggerFactory.getLogger(ParametrosService.class);

    private final ParametroRepository repository;
    private final AuditoriaService auditoria;

    /** Ultimo valor leido de cada parametro. Se rellena al primer uso. */
    private volatile Map<ParametroInstitucional, String> cache;

    public ParametrosService(ParametroRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    // ---------------------------------------------------------------------
    // Lectura tipada (lo que usa el resto del sistema)
    // ---------------------------------------------------------------------

    /** RB-03 / RF-59: escala vigente. */
    public EscalaCalificacion escalaCalificacion() {
        try {
            return new EscalaCalificacion(
                    decimal(ParametroInstitucional.NOTA_MINIMA),
                    decimal(ParametroInstitucional.NOTA_MAXIMA),
                    decimal(ParametroInstitucional.NOTA_APROBATORIA));
        } catch (ReglaNegocioException ex) {
            // Una escala invalida en base de datos no puede dejar el sistema sin
            // poder calificar: se registra y se sigue con la del enunciado.
            log.error("Escala de calificacion invalida en parametros; se usa la de por defecto: {}",
                    ex.getMessage());
            return EscalaCalificacion.POR_DEFECTO;
        }
    }

    /** RB-04 / RF-59: porcentaje minimo de asistencia vigente. */
    public double porcentajeMinimoAsistencia() {
        return decimal(ParametroInstitucional.PORCENTAJE_MINIMO_ASISTENCIA).doubleValue();
    }

    /** RB-09 / RF-59: maximo de horas semanales por docente. */
    public int maxHorasDocente() {
        return decimal(ParametroInstitucional.MAX_HORAS_DOCENTE).intValue();
    }

    // ---------------------------------------------------------------------
    // Consulta y actualizacion (RF-59)
    // ---------------------------------------------------------------------

    /** Todos los parametros con su valor vigente. */
    @Transactional(readOnly = true)
    public Map<ParametroInstitucional, String> listar() {
        return new EnumMap<>(valores());
    }

    /**
     * RF-59 / RS-07: fija el valor de un parametro.
     *
     * <p>Cambiar la escala recalcula la aprobacion de todo el mundo y cambiar el
     * minimo de asistencia decide quien pierde el derecho a evaluacion, de modo
     * que es una operacion critica aunque no toque ninguna nota.</p>
     */
    @Transactional
    public void actualizar(ParametroInstitucional clave, String valor) {
        String anterior = valores().getOrDefault(clave, clave.getValorPorDefecto());
        validar(clave, valor);

        // La escala se valida como conjunto, no parametro a parametro: subir la
        // nota aprobatoria por encima del maximo es valido en aislamiento y deja
        // una escala en la que nadie puede aprobar.
        if (esDeLaEscala(clave)) {
            validarEscalaResultante(clave, valor);
        }

        ParametroJpaEntity e = new ParametroJpaEntity();
        e.setClave(clave);
        e.setValor(valor.trim());
        e.setActualizado(LocalDateTime.now());
        e.setActualizadoPor(usuarioActual());
        repository.save(e);
        cache = null; // se relee al proximo uso

        auditoria.registrar(TipoOperacion.PARAMETRO_ACTUALIZADO, "parametro_institucional", null,
                "Parametro " + clave + " cambiado de " + anterior + " a " + valor.trim() + ".");
    }

    // ---------------------------------------------------------------------
    // Interno
    // ---------------------------------------------------------------------

    private void validar(ParametroInstitucional clave, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("RF-59", "El valor del parametro es obligatorio.");
        }
        BigDecimal numero;
        try {
            numero = new BigDecimal(valor.trim());
        } catch (NumberFormatException ex) {
            throw new ReglaNegocioException("RF-59", "El valor de " + clave + " debe ser numerico.");
        }
        switch (clave.getTipo()) {
            case DECIMAL -> exigir(numero.compareTo(BigDecimal.ZERO) > 0, clave,
                    "debe ser mayor que cero");
            case PORCENTAJE -> exigir(numero.compareTo(BigDecimal.ZERO) >= 0
                            && numero.compareTo(new BigDecimal("100")) <= 0, clave,
                    "debe estar entre 0 y 100");
            case ENTERO_POSITIVO -> exigir(numero.stripTrailingZeros().scale() <= 0
                            && numero.compareTo(BigDecimal.ZERO) > 0, clave,
                    "debe ser un entero mayor que cero");
        }
    }

    private static void exigir(boolean condicion, ParametroInstitucional clave, String queSeEspera) {
        if (!condicion) {
            throw new ReglaNegocioException("RF-59", "El valor de " + clave + " " + queSeEspera + ".");
        }
    }

    private static boolean esDeLaEscala(ParametroInstitucional clave) {
        return clave == ParametroInstitucional.NOTA_MINIMA
                || clave == ParametroInstitucional.NOTA_MAXIMA
                || clave == ParametroInstitucional.NOTA_APROBATORIA;
    }

    /** Construye la escala que quedaria tras el cambio; su constructor la valida. */
    private void validarEscalaResultante(ParametroInstitucional clave, String valor) {
        Map<ParametroInstitucional, String> resultante = new EnumMap<>(valores());
        resultante.put(clave, valor.trim());
        new EscalaCalificacion(
                new BigDecimal(resultante.getOrDefault(ParametroInstitucional.NOTA_MINIMA,
                        ParametroInstitucional.NOTA_MINIMA.getValorPorDefecto())),
                new BigDecimal(resultante.getOrDefault(ParametroInstitucional.NOTA_MAXIMA,
                        ParametroInstitucional.NOTA_MAXIMA.getValorPorDefecto())),
                new BigDecimal(resultante.getOrDefault(ParametroInstitucional.NOTA_APROBATORIA,
                        ParametroInstitucional.NOTA_APROBATORIA.getValorPorDefecto())));
    }

    private BigDecimal decimal(ParametroInstitucional clave) {
        String valor = valores().get(clave);
        try {
            return valor == null ? clave.decimalPorDefecto() : new BigDecimal(valor);
        } catch (NumberFormatException ex) {
            log.error("Parametro {} con valor no numerico ({}); se usa el de por defecto.", clave, valor);
            return clave.decimalPorDefecto();
        }
    }

    /**
     * Valores vigentes, leyendo de la base solo la primera vez.
     *
     * <p>Si la tabla aun no tiene una clave (base anterior a V15, o un parametro
     * anadido despues), se usa el valor por defecto del enum en vez de fallar:
     * un sistema que no arranca porque falta una fila de configuracion es peor
     * que uno que arranca con el valor del enunciado.</p>
     */
    private Map<ParametroInstitucional, String> valores() {
        Map<ParametroInstitucional, String> actual = cache;
        if (actual != null) {
            return actual;
        }
        Map<ParametroInstitucional, String> leidos = new EnumMap<>(ParametroInstitucional.class);
        for (ParametroInstitucional clave : ParametroInstitucional.values()) {
            leidos.put(clave, clave.getValorPorDefecto());
        }
        List<ParametroJpaEntity> filas = repository.findAll();
        for (ParametroJpaEntity fila : filas) {
            leidos.put(fila.getClave(), fila.getValor());
        }
        cache = leidos;
        return leidos;
    }

    private static String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : AuditoriaService.USUARIO_SISTEMA;
    }
}
