package com.educktrack.usuarios.domain;

/**
 * Roles institucionales del sistema (RD-07, RS-03).
 *
 * <p>Determinan el acceso por RBAC. La combinacion ESTUDIANTE + DOCENTE en una
 * misma cuenta esta prohibida (RB-14), invariante que valida {@link Usuario}.</p>
 */
public enum NombreRol {
    ADMINISTRADOR,
    RECTOR,
    COORDINADOR_ACADEMICO,
    COORDINADOR_CONVIVENCIA,
    DOCENTE,
    ESTUDIANTE,
    PADRE_FAMILIA
}
