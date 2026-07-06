-- ==========================================================================
-- V6 - Modulo de calificaciones (Fase 6)
-- RF-20 (ponderacion), RF-31..RF-37 (notas, corte, novedad, boletin).
-- Reglas: RB-03 (escala 1-5, en app), RB-07 (ponderaciones suman 100%),
-- RB-15 (novedad auditada), RB-19 (cierre de corte).
-- ==========================================================================

-- ---------- Ponderacion de evaluaciones (RF-20, RB-07) ----------
CREATE TABLE ponderacion_evaluacion (
    id                    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    materia_id            BIGINT      NOT NULL,
    periodo_academico_id  BIGINT      NOT NULL,
    tipo                  VARCHAR(20) NOT NULL,
    porcentaje            INT         NOT NULL,
    CONSTRAINT uq_ponderacion UNIQUE (materia_id, periodo_academico_id, tipo),
    CONSTRAINT fk_pond_materia FOREIGN KEY (materia_id)           REFERENCES materia (id),
    CONSTRAINT fk_pond_periodo FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Calificacion (RF-31) ----------
CREATE TABLE calificacion (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    estudiante_id         BIGINT       NOT NULL,
    materia_id            BIGINT       NOT NULL,
    curso_id              BIGINT       NOT NULL,
    periodo_academico_id  BIGINT       NOT NULL,
    tipo                  VARCHAR(20)  NOT NULL,
    valor                 DOUBLE       NOT NULL,
    descripcion           VARCHAR(200) NULL,
    fecha_registro        DATETIME(6)  NOT NULL,
    CONSTRAINT fk_cal_estudiante FOREIGN KEY (estudiante_id)        REFERENCES estudiante (id),
    CONSTRAINT fk_cal_materia    FOREIGN KEY (materia_id)           REFERENCES materia (id),
    CONSTRAINT fk_cal_curso      FOREIGN KEY (curso_id)             REFERENCES curso (id),
    CONSTRAINT fk_cal_periodo    FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id),
    INDEX idx_cal_est_mat_per (estudiante_id, materia_id, periodo_academico_id),
    INDEX idx_cal_est_per (estudiante_id, periodo_academico_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Cierre de corte (RF-34, RB-19) ----------
CREATE TABLE cierre_corte (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    curso_id              BIGINT       NOT NULL,
    periodo_academico_id  BIGINT       NOT NULL,
    fecha_cierre          DATETIME(6)  NOT NULL,
    cerrado_por           VARCHAR(150) NOT NULL,
    CONSTRAINT uq_cierre_curso_periodo UNIQUE (curso_id, periodo_academico_id),
    CONSTRAINT fk_cierre_curso   FOREIGN KEY (curso_id)             REFERENCES curso (id),
    CONSTRAINT fk_cierre_periodo FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Novedad de nota (RF-36, RB-15) ----------
CREATE TABLE novedad_nota (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    calificacion_id  BIGINT       NOT NULL,
    valor_anterior   DOUBLE       NOT NULL,
    valor_nuevo      DOUBLE       NOT NULL,
    motivo           VARCHAR(300) NOT NULL,
    usuario          VARCHAR(150) NOT NULL,
    fecha            DATETIME(6)  NOT NULL,
    CONSTRAINT fk_novedad_calificacion FOREIGN KEY (calificacion_id) REFERENCES calificacion (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
