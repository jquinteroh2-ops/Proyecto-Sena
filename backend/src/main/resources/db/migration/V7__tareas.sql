-- ==========================================================================
-- V7 - Modulo de tareas (Fase 7)
-- RF-38 (asignar), RF-39 (entregar), RF-40 (calificar), RF-41 (estado).
-- Regla: RB-10 (entrega dentro de plazo salvo autorizacion), en la app.
-- ==========================================================================

CREATE TABLE tarea (
    id                      BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    titulo                  VARCHAR(150)  NOT NULL,
    descripcion             VARCHAR(1000) NULL,
    materia_id              BIGINT        NOT NULL,
    curso_id                BIGINT        NOT NULL,
    periodo_academico_id    BIGINT        NOT NULL,
    docente_id              BIGINT        NULL,
    fecha_limite            DATE          NOT NULL,
    permite_entrega_tardia  BIT(1)        NOT NULL DEFAULT b'0',
    fecha_creacion          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_tarea_materia FOREIGN KEY (materia_id)           REFERENCES materia (id),
    CONSTRAINT fk_tarea_curso   FOREIGN KEY (curso_id)             REFERENCES curso (id),
    CONSTRAINT fk_tarea_periodo FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id),
    CONSTRAINT fk_tarea_docente FOREIGN KEY (docente_id)           REFERENCES docente (id),
    INDEX idx_tarea_curso (curso_id),
    INDEX idx_tarea_fecha_limite (fecha_limite)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE entrega_tarea (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tarea_id           BIGINT        NOT NULL,
    estudiante_id      BIGINT        NOT NULL,
    evidencia          VARCHAR(1000) NULL,
    fecha_entrega      DATETIME(6)   NOT NULL,
    calificacion       DOUBLE        NULL,
    retroalimentacion  VARCHAR(1000) NULL,
    CONSTRAINT uq_entrega_tarea_estudiante UNIQUE (tarea_id, estudiante_id),
    CONSTRAINT fk_entrega_tarea      FOREIGN KEY (tarea_id)      REFERENCES tarea (id) ON DELETE CASCADE,
    CONSTRAINT fk_entrega_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiante (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
