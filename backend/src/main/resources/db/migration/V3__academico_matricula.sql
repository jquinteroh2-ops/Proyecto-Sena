-- ==========================================================================
-- V3 - Modulo academico: matricula, plan de estudios y asignacion docente (Fase 3)
-- RF-09 (matricula), RF-14 (asignacion), RF-19 (plan de estudios).
-- Reglas: RB-01 (matricula activa unica por periodo, reforzada en app),
-- RB-11 (plan de estudios), RB-16/RB-02 (asignacion/director).
-- ==========================================================================

-- Datos de retiro del estudiante (RB-20 / RF-10)
ALTER TABLE estudiante
    ADD COLUMN motivo_retiro     VARCHAR(300)  NULL,
    ADD COLUMN fecha_retiro      DATE          NULL,
    ADD COLUMN autorizado_retiro VARCHAR(120)  NULL;

-- ---------- Plan de estudios: materias por curso (RD-05, RB-11) ----------
CREATE TABLE plan_estudios (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    curso_id   BIGINT NOT NULL,
    materia_id BIGINT NOT NULL,
    CONSTRAINT uq_plan_curso_materia UNIQUE (curso_id, materia_id),
    CONSTRAINT fk_plan_curso   FOREIGN KEY (curso_id)   REFERENCES curso (id)   ON DELETE CASCADE,
    CONSTRAINT fk_plan_materia FOREIGN KEY (materia_id) REFERENCES materia (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Matricula (RF-09) ----------
CREATE TABLE matricula (
    id                    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    estudiante_id         BIGINT      NOT NULL,
    curso_id              BIGINT      NOT NULL,
    periodo_academico_id  BIGINT      NOT NULL,
    fecha_matricula       DATE        NOT NULL,
    estado                VARCHAR(20) NOT NULL,
    CONSTRAINT fk_matricula_estudiante FOREIGN KEY (estudiante_id)        REFERENCES estudiante (id),
    CONSTRAINT fk_matricula_curso      FOREIGN KEY (curso_id)             REFERENCES curso (id),
    CONSTRAINT fk_matricula_periodo    FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id),
    INDEX idx_matricula_estudiante_periodo (estudiante_id, periodo_academico_id),
    INDEX idx_matricula_curso_estado (curso_id, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Asignacion academica de docente (RF-14, RB-16) ----------
CREATE TABLE asignacion_docente (
    id                    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    docente_id            BIGINT NOT NULL,
    materia_id            BIGINT NOT NULL,
    curso_id              BIGINT NOT NULL,
    periodo_academico_id  BIGINT NOT NULL,
    CONSTRAINT uq_asignacion UNIQUE (docente_id, materia_id, curso_id, periodo_academico_id),
    CONSTRAINT fk_asig_docente FOREIGN KEY (docente_id)           REFERENCES docente (id),
    CONSTRAINT fk_asig_materia FOREIGN KEY (materia_id)           REFERENCES materia (id),
    CONSTRAINT fk_asig_curso   FOREIGN KEY (curso_id)             REFERENCES curso (id),
    CONSTRAINT fk_asig_periodo FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
