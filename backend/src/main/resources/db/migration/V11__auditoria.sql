-- ==========================================================================
-- V11 - Log de auditoria (Fase 4)
-- RS-07 (toda operacion critica queda registrada con usuario, fecha y
-- descripcion), RF-63 (registrarla automaticamente), RF-05 (historial de
-- inicios de sesion).
--
-- Una sola tabla para ambas cosas: un inicio de sesion es una operacion
-- critica mas, y separarla en otra tabla obligaria a consultar en dos sitios
-- para responder "que hizo esta persona". El tipo de operacion basta para
-- filtrar el historial de accesos que pide RF-05.
--
-- 'usuario' se guarda como el correo institucional en texto, no como clave
-- foranea a 'usuario': el registro debe sobrevivir al borrado de la cuenta y
-- debe poder anotar intentos de acceso con correos que no existen. Un log de
-- auditoria que se puede quedar sin filas por un ON DELETE no sirve para
-- auditar.
--
-- No hay indice sobre 'fecha' sola: las consultas siempre acotan por usuario o
-- por tipo de operacion, y ambos indices ya llevan la fecha para ordenar.
-- ==========================================================================

CREATE TABLE auditoria (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario      VARCHAR(150) NOT NULL,
    operacion    VARCHAR(40)  NOT NULL,
    entidad      VARCHAR(60)  NULL,
    entidad_id   BIGINT       NULL,
    descripcion  VARCHAR(500) NOT NULL,
    fecha        DATETIME(6)  NOT NULL,
    -- RF-05: "que hizo esta cuenta", en orden cronologico inverso.
    INDEX idx_auditoria_usuario_fecha (usuario, fecha),
    -- RS-07: "todos los cambios de nota", "todos los accesos fallidos".
    INDEX idx_auditoria_operacion_fecha (operacion, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
