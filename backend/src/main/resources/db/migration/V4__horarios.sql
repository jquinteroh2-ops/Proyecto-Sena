-- ==========================================================================
-- V4 - Modulo de horarios (Fase 4)
-- Bloques horarios (RF-21) y asignaciones (RF-22). La validacion de cruces
-- (RB-18 / RF-23) se aplica en la capa de aplicacion.
-- ==========================================================================

-- ---------- Bloque horario (RF-21, HU-11) ----------
CREATE TABLE bloque_horario (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dia          VARCHAR(12) NOT NULL,
    hora_inicio  TIME        NOT NULL,
    hora_fin     TIME        NOT NULL,
    jornada      VARCHAR(20) NOT NULL,
    CONSTRAINT uq_bloque UNIQUE (dia, hora_inicio, hora_fin, jornada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Asignacion horaria (RF-22) ----------
CREATE TABLE asignacion_horaria (
    id                    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bloque_id             BIGINT NOT NULL,
    materia_id            BIGINT NOT NULL,
    docente_id            BIGINT NOT NULL,
    curso_id              BIGINT NOT NULL,
    periodo_academico_id  BIGINT NOT NULL,
    CONSTRAINT fk_ah_bloque  FOREIGN KEY (bloque_id)            REFERENCES bloque_horario (id),
    CONSTRAINT fk_ah_materia FOREIGN KEY (materia_id)           REFERENCES materia (id),
    CONSTRAINT fk_ah_docente FOREIGN KEY (docente_id)           REFERENCES docente (id),
    CONSTRAINT fk_ah_curso   FOREIGN KEY (curso_id)             REFERENCES curso (id),
    CONSTRAINT fk_ah_periodo FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id),
    -- RB-18: refuerzo en BD del cruce exacto por bloque/periodo
    CONSTRAINT uq_ah_docente_bloque UNIQUE (docente_id, bloque_id, periodo_academico_id),
    CONSTRAINT uq_ah_curso_bloque   UNIQUE (curso_id, bloque_id, periodo_academico_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
