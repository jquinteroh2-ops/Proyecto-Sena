-- ==========================================================================
-- V9 - Identidad: vinculo entre cuentas de usuario y perfiles (Fase 1)
--
-- Hasta V8 no existia ninguna relacion entre la tabla 'usuario' y los perfiles
-- 'estudiante' / 'docente', por lo que el RBAC (RS-03) no podia resolver a que
-- registro corresponde el usuario autenticado y RB-08 era inaplicable.
--
-- Esta migracion es puramente aditiva: no altera ni elimina datos existentes.
--   * estudiante.usuario_id / docente.usuario_id -> relacion 1:1 opcional.
--     NULL permitido: un estudiante se registra y matricula (RF-06, RF-09)
--     antes de tener cuenta. UNIQUE garantiza el 1:1 cuando el vinculo existe
--     (MySQL admite multiples NULL en un indice UNIQUE).
--   * vinculo_acudiente -> RF-11 (vincular padre a estudiante), RB-08 (el padre
--     solo ve estudiantes formalmente vinculados) y RD-08 (parentesco).
--
-- Los campos acudiente_nombre / acudiente_telefono / acudiente_parentesco de
-- 'estudiante' se conservan: cubren HU-05 (dato de contacto exigido en el
-- registro, cuando aun no hay cuenta) y son independientes del vinculo formal
-- con una cuenta que introduce esta migracion.
-- ==========================================================================

-- ---------- Cuenta del estudiante (RS-03, RNF-07) ----------
ALTER TABLE estudiante
    ADD COLUMN usuario_id BIGINT NULL,
    ADD CONSTRAINT uq_estudiante_usuario UNIQUE (usuario_id),
    ADD CONSTRAINT fk_estudiante_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id);

-- ---------- Cuenta del docente (RS-03, RNF-07) ----------
ALTER TABLE docente
    ADD COLUMN usuario_id BIGINT NULL,
    ADD CONSTRAINT uq_docente_usuario UNIQUE (usuario_id),
    ADD CONSTRAINT fk_docente_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id);

-- ---------- Vinculo acudiente <-> estudiante (RF-11, RB-08, RD-08) ----------
CREATE TABLE vinculo_acudiente (
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL,
    estudiante_id  BIGINT      NOT NULL,
    parentesco     VARCHAR(25) NOT NULL,
    fecha_vinculo  DATETIME(6) NOT NULL,
    -- RB-08: un mismo acudiente no puede vincularse dos veces al mismo estudiante.
    CONSTRAINT uq_vinculo_acudiente UNIQUE (usuario_id, estudiante_id),
    CONSTRAINT fk_vinculo_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_vinculo_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiante (id),
    -- Consulta caliente: "que estudiantes tutela el usuario autenticado" (RB-08).
    INDEX idx_vinculo_usuario (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
