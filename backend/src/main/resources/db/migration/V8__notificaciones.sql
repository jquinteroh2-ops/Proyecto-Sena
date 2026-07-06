-- ==========================================================================
-- V8 - Modulo de notificaciones (Fase 8)
-- RF-52 (canal), RF-53 (envio manual), RF-54 (bandeja), RF-55/56 (auto).
-- ==========================================================================

CREATE TABLE notificacion (
    id                       BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    destinatario_usuario_id  BIGINT        NOT NULL,
    titulo                   VARCHAR(150)  NOT NULL,
    mensaje                  VARCHAR(1000) NOT NULL,
    tipo                     VARCHAR(30)   NOT NULL,
    leida                    BIT(1)        NOT NULL DEFAULT b'0',
    fecha_creacion           DATETIME(6)   NOT NULL,
    CONSTRAINT fk_notif_usuario FOREIGN KEY (destinatario_usuario_id) REFERENCES usuario (id),
    INDEX idx_notif_destinatario (destinatario_usuario_id, fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Configuracion institucional del canal (fila unica id=1, HU-28)
CREATE TABLE configuracion_notificacion (
    id     INT         NOT NULL PRIMARY KEY,
    canal  VARCHAR(12) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO configuracion_notificacion (id, canal) VALUES (1, 'INTERNO');
