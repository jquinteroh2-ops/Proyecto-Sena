package com.educktrack.auditoria.domain;

/**
 * Operaciones criticas que el sistema registra en el log de auditoria (RS-07).
 *
 * <p>RS-07 nombra explicitamente "cambios de nota, retiros y cambios de rol";
 * el resto sale de los criterios de aceptacion de las historias de usuario, que
 * exigen registro de auditoria en HU-02 (desactivar cuenta), HU-07 (retiro de
 * estudiante), HU-10 (ponderacion), HU-15 (justificar inasistencia), HU-20
 * (cierre de corte) y HU-22 (novedad de nota).</p>
 *
 * <p>El enum es cerrado a proposito: obliga a que anadir una operacion auditada
 * sea una decision explicita y mantiene los valores estables para poder
 * filtrar el log por tipo (RF-05).</p>
 */
public enum TipoOperacion {

    /** RF-05: inicio de sesion correcto. */
    ACCESO_EXITOSO,

    /**
     * RF-05: intento de inicio de sesion rechazado. Se registra porque una
     * racha de fallos sobre una misma cuenta es justo lo que un historial de
     * accesos debe permitir ver.
     */
    ACCESO_FALLIDO,

    /** RF-01 / RS-07: alta de cuenta, con los roles asignados. */
    USUARIO_CREADO,

    /** RF-03 / HU-02: desactivacion de una cuenta. */
    USUARIO_DESACTIVADO,

    /** RF-10 / HU-07: retiro de un estudiante, con motivo y autorizacion. */
    ESTUDIANTE_RETIRADO,

    /** RF-09: matricula de un estudiante en un curso. */
    MATRICULA_REGISTRADA,

    /** RF-09 / RB-01: anulacion de una matricula. */
    MATRICULA_ANULADA,

    /** RF-31 / RS-07: registro de una calificacion. */
    NOTA_REGISTRADA,

    /** RF-32 / RS-07: edicion de una calificacion con el corte abierto. */
    NOTA_EDITADA,

    /** RF-36 / HU-22 / RB-15: correccion de una nota de un corte cerrado. */
    NOTA_NOVEDAD,

    /** RF-34 / HU-20 / RB-19: cierre de corte academico. */
    CORTE_CERRADO,

    /** RF-27 / HU-15: justificacion de una inasistencia. */
    ASISTENCIA_JUSTIFICADA,

    /** RF-20 / HU-10 / RB-07: configuracion de ponderaciones. */
    PONDERACION_CONFIGURADA,

    /** RF-64 / HU-04: se emitio un enlace de recuperacion de contrasena. */
    RECUPERACION_SOLICITADA,

    /**
     * RF-64 / HU-04: intento de recuperacion que no prospero (correo sin cuenta
     * activa, o enlace invalido, caducado o ya usado). HU-04 pide expresamente
     * que estos intentos queden registrados: son la senal de que alguien esta
     * tanteando cuentas ajenas.
     */
    RECUPERACION_FALLIDA,

    /** RF-64 / HU-04: contrasena restablecida mediante un enlace valido. */
    PASSWORD_RESTABLECIDA,

    /**
     * RF-59 / RS-14: cambio de un parametro institucional. Cambiar la escala
     * recalcula la aprobacion de todo el mundo y cambiar el minimo de
     * asistencia decide quien pierde el derecho a evaluacion, de modo que es
     * critico aunque no toque ninguna nota.
     */
    PARAMETRO_ACTUALIZADO;

    /** RF-05: distingue los eventos de sesion del resto del log. */
    public boolean esAcceso() {
        return this == ACCESO_EXITOSO || this == ACCESO_FALLIDO;
    }
}
