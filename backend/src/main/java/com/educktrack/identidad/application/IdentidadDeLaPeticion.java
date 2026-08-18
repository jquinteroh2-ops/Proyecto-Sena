package com.educktrack.identidad.application;

import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Memoria de identidad acotada a una peticion (RNF-03: tiempo de respuesta).
 *
 * <p>El control de acceso de la Fase 2 resuelve la identidad consultando la
 * base de datos y no el token, que es lo correcto: incrustar identificadores en
 * el JWT deja tokens desincronizados cuando cambia un vinculo. El precio es que
 * una sola llamada a {@code puedeVerEstudiante} llegaba a leer la cuenta
 * <strong>cuatro veces</strong> —{@code tieneVisionInstitucional},
 * {@code usuarioIdActual} y {@code cursosDelDocente} la recargaban cada una— y
 * los servicios encadenan varias comprobaciones por peticion.</p>
 *
 * <p>Dentro de una peticion la respuesta no puede cambiar: el correo del token
 * es fijo y la cuenta no se modifica a mitad de la llamada. Memorizarla es
 * exacto, no una aproximacion.</p>
 *
 * <h2>Por que un ThreadLocal y no un bean de ambito 'request'</h2>
 *
 * <p>Un bean {@code @RequestScope} falla fuera de una peticion HTTP, y
 * {@link ContextoUsuario} tambien se usa desde listeners de eventos. Con un
 * ThreadLocal, quien corre fuera de una peticion simplemente no encuentra nada
 * memorizado y consulta como antes.</p>
 *
 * <p><strong>Lo limpia {@code LimpiezaIdentidadFilter} en un {@code finally}.</strong>
 * Sin eso, el hilo se devuelve al pool con la identidad de quien acaba de pasar
 * por el, y la siguiente peticion que lo reutilice heredaria una identidad
 * ajena: un fallo de aislamiento, no de rendimiento. La clave incluye el correo
 * precisamente para que ese error no pueda pasar inadvertido.</p>
 */
public final class IdentidadDeLaPeticion {

    private static final ThreadLocal<Memoria> MEMORIA = new ThreadLocal<>();

    private IdentidadDeLaPeticion() {
    }

    /**
     * Devuelve la cuenta memorizada para ese correo, o la resuelve y la
     * memoriza. Si el correo no coincide con el memorizado, descarta lo
     * anterior: la identidad de la peticion es la del correo que se pregunta.
     */
    static UsuarioJpaEntity usuario(String correo, Supplier<UsuarioJpaEntity> resolver) {
        Memoria memoria = MEMORIA.get();
        if (memoria != null && memoria.correo.equals(correo)) {
            return memoria.usuario;
        }
        UsuarioJpaEntity usuario = resolver.get();
        MEMORIA.set(new Memoria(correo, usuario));
        return usuario;
    }

    /** Perfil de estudiante asociado a la cuenta, memorizado igual que la cuenta. */
    static Optional<Long> estudianteId(String correo, Supplier<Optional<Long>> resolver) {
        Memoria memoria = MEMORIA.get();
        if (memoria != null && memoria.correo.equals(correo) && memoria.estudianteId != null) {
            return memoria.estudianteId;
        }
        Optional<Long> resuelto = resolver.get();
        memorizarPerfil(correo, m -> m.estudianteId = resuelto);
        return resuelto;
    }

    /** Perfil de docente asociado a la cuenta, memorizado igual que la cuenta. */
    static Optional<Long> docenteId(String correo, Supplier<Optional<Long>> resolver) {
        Memoria memoria = MEMORIA.get();
        if (memoria != null && memoria.correo.equals(correo) && memoria.docenteId != null) {
            return memoria.docenteId;
        }
        Optional<Long> resuelto = resolver.get();
        memorizarPerfil(correo, m -> m.docenteId = resuelto);
        return resuelto;
    }

    /**
     * Guarda el perfil recien resuelto en la memoria del hilo, si la hay.
     *
     * <p>Se consulta la memoria <strong>despues</strong> de ejecutar el
     * resolver, y no antes, porque el propio resolver la crea: para saber que
     * perfil corresponde a la cuenta hay que resolver primero la cuenta. Mirando
     * solo antes, la primera llamada siempre encontraba el hilo vacio y tiraba
     * el resultado.</p>
     */
    private static void memorizarPerfil(String correo, java.util.function.Consumer<Memoria> asignar) {
        Memoria memoria = MEMORIA.get();
        if (memoria != null && memoria.correo.equals(correo)) {
            asignar.accept(memoria);
        }
    }

    /** Vacia la memoria del hilo actual. Obligatorio al terminar la peticion. */
    public static void limpiar() {
        MEMORIA.remove();
    }

    private static final class Memoria {
        private final String correo;
        private final UsuarioJpaEntity usuario;
        private Optional<Long> estudianteId;
        private Optional<Long> docenteId;

        private Memoria(String correo, UsuarioJpaEntity usuario) {
            this.correo = correo;
            this.usuario = usuario;
        }
    }
}
