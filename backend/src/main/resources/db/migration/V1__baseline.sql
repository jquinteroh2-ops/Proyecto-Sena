-- ==========================================================================
-- V1 - Baseline del esquema EduckTrack
-- Flyway gestiona el esquema como unica fuente de verdad (RS-02).
-- Las entidades de dominio (Usuario, Rol, Estudiante, Docente, Curso, Materia,
-- PeriodoAcademico) se agregan en la Fase 1 mediante migraciones V2+.
-- ==========================================================================

-- Tabla de metadatos de la aplicacion (no mapeada a entidad JPA).
CREATE TABLE app_metadata (
    clave       VARCHAR(64)  NOT NULL PRIMARY KEY,
    valor       VARCHAR(255) NOT NULL,
    actualizado TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO app_metadata (clave, valor) VALUES
    ('schema_version', 'baseline'),
    ('proyecto', 'EduckTrack');
