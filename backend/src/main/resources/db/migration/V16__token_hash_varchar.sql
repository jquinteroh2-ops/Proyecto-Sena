-- ==========================================================================
-- V16 - token_recuperacion.token_hash pasa de CHAR(64) a VARCHAR(64)
--
-- V13 declaro la columna como CHAR(64), que es lo natural para un hash SHA-256
-- en hexadecimal: siempre mide exactamente 64 caracteres. Pero la entidad JPA
-- la mapea con @Column(length = 64), que Hibernate traduce a VARCHAR, y
-- 'ddl-auto: validate' aborta el arranque al encontrar CHAR donde espera
-- VARCHAR. El backend no llego a levantar en el despliegue del 18/08/2026.
--
-- Se alinea la base con la entidad y no al reves: poner columnDefinition =
-- "char(64)" en la entidad meteria sintaxis especifica del motor en el modelo,
-- y las pruebas generan el esquema con H2. La diferencia de almacenamiento
-- entre CHAR(64) y VARCHAR(64) es un byte de longitud por fila.
--
-- Por que una migracion nueva y no corregir V13: V13 ya se aplico contra la
-- base real. Editar una migracion ejecutada rompe la suma de verificacion de
-- Flyway y deja el esquema sin una historia unica.
--
-- POR QUE NO SE DETECTO ANTES: las pruebas usan H2 con create-drop y Flyway
-- desactivado, de modo que el esquema se genera A PARTIR de las entidades y
-- siempre coincide. Nadie ejecuta las migraciones antes de desplegar. Es
-- exactamente el fallo que Testcontainers evitaria (ver seccion 4).
-- ==========================================================================

ALTER TABLE token_recuperacion
    MODIFY COLUMN token_hash VARCHAR(64) NOT NULL;
