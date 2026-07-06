-- ==========================================================================
-- V5 - Modulo de asistencia (Fase 5)
-- RF-26 (registro), RF-27 (justificacion), RF-28/RF-30 (reportes/alertas).
-- Reglas: RB-06 (registro unico por bloque/fecha, edicion 48h) y RB-04
-- (porcentaje minimo del 80%, aplicadas en la capa de aplicacion).
-- ==========================================================================

CREATE TABLE asistencia (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    estudiante_id         BIGINT       NOT NULL,
    curso_id              BIGINT       NOT NULL,
    materia_id            BIGINT       NOT NULL,
    bloque_id             BIGINT       NOT NULL,
    periodo_academico_id  BIGINT       NOT NULL,
    fecha                 DATE         NOT NULL,
    estado                VARCHAR(12)  NOT NULL,
    justificada           BIT(1)       NOT NULL DEFAULT b'0',
    motivo_justificacion  VARCHAR(300) NULL,
    fecha_registro        DATETIME(6)  NOT NULL,
    CONSTRAINT uq_asistencia UNIQUE (estudiante_id, bloque_id, fecha),
    CONSTRAINT fk_asis_estudiante FOREIGN KEY (estudiante_id)        REFERENCES estudiante (id),
    CONSTRAINT fk_asis_curso      FOREIGN KEY (curso_id)             REFERENCES curso (id),
    CONSTRAINT fk_asis_materia    FOREIGN KEY (materia_id)           REFERENCES materia (id),
    CONSTRAINT fk_asis_bloque     FOREIGN KEY (bloque_id)            REFERENCES bloque_horario (id),
    CONSTRAINT fk_asis_periodo    FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id),
    INDEX idx_asis_estudiante_materia_periodo (estudiante_id, materia_id, periodo_academico_id),
    INDEX idx_asis_curso_materia_periodo (curso_id, materia_id, periodo_academico_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
