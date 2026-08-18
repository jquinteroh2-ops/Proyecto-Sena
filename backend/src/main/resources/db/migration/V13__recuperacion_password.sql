-- ==========================================================================
-- V13 - Enlaces de recuperacion de contrasena (Fase 7)
-- RF-64 (restablecer la contrasena mediante verificacion por correo) y HU-04:
-- enlace valido 30 minutos, de un solo uso, y los intentos fallidos quedan
-- registrados.
--
-- 'token_hash' guarda el HASH del token, nunca el token. Un enlace de
-- recuperacion es una credencial: mientras esta vivo permite tomar el control
-- de la cuenta sin conocer la contrasena. Guardarlo en claro significaria que
-- cualquiera con lectura sobre esta tabla (una copia de seguridad, un volcado
-- de soporte) puede entrar como cualquier usuario que este recuperando. Es el
-- mismo motivo por el que la contrasena se guarda cifrada (RS-05).
--
-- El hash es SHA-256 y no BCrypt, a diferencia de la contrasena. BCrypt es
-- lento a proposito para que no se pueda adivinar un secreto de baja entropia
-- elegido por una persona; aqui el token son 256 bits aleatorios, no hay
-- diccionario que probar, y ademas BCrypt lleva sal propia, lo que obligaria a
-- recorrer la tabla fila por fila en vez de buscar por indice.
--
-- 'fecha_uso' NULL es lo que expresa "sin usar": el enlace expira al usarse una
-- sola vez (HU-04). No se borra la fila al consumirla, porque un token gastado
-- que reaparece es justo lo que interesa poder auditar.
-- ==========================================================================

CREATE TABLE token_recuperacion (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL,
    token_hash       CHAR(64)     NOT NULL,
    fecha_solicitud  DATETIME(6)  NOT NULL,
    fecha_expiracion DATETIME(6)  NOT NULL,
    fecha_uso        DATETIME(6)  NULL,
    -- La busqueda al restablecer es siempre por el hash del token recibido.
    CONSTRAINT uq_token_recuperacion_hash UNIQUE (token_hash),
    -- Al solicitar un enlace nuevo se invalidan los anteriores de esa cuenta,
    -- de modo que nunca haya dos enlaces vivos a la vez.
    INDEX idx_token_recuperacion_usuario (usuario_id, fecha_uso),
    CONSTRAINT fk_token_recuperacion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
