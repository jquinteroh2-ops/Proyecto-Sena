-- ==========================================================================
-- V10 - Integridad de RB-01 y RB-05 en la base de datos (Fase 3)
--
-- Hasta aqui ambas reglas se comprobaban unicamente en la capa de aplicacion,
-- con un SELECT previo al INSERT. Ese patron no es atomico: dos peticiones
-- concurrentes pueden leer "no existe" a la vez y crear las dos filas que la
-- regla prohibe. Con matriculas de inicio de curso, donde varias personas de
-- Coordinacion trabajan al mismo tiempo, no es un caso hipotetico.
--
-- MySQL no admite indices unicos parciales (no existe WHERE en un indice), asi
-- que la restriccion se expresa con una columna generada que vale NULL cuando
-- la fila no participa de la regla: un indice UNIQUE admite tantos NULL como
-- haga falta, de modo que solo colisionan las filas que si deben ser unicas.
--
-- Las columnas generadas no rompen 'ddl-auto: validate': Hibernate comprueba
-- que existan las columnas mapeadas, no que no haya otras. Por eso NO se
-- anaden a las entidades JPA: son una restriccion del esquema, no un dato del
-- dominio, y mapearlas invitaria a escribir en ellas.
--
-- Migracion aditiva: no altera ni elimina datos existentes. Si en una base ya
-- poblada hubiera filas que violan la regla, el ALTER falla al crear el indice;
-- eso es lo deseable (avisa en vez de corromper), pero obliga a limpiar los
-- duplicados antes de aplicarla.
-- ==========================================================================

-- ---------- RB-01: una sola matricula ACTIVA por estudiante y periodo -------
-- "Un estudiante solo puede estar matriculado en un curso activo por periodo
-- academico." Las matriculas RETIRADA y FINALIZADA quedan fuera del indice
-- (valor NULL), que es lo que permite volver a matricular tras un retiro.
ALTER TABLE matricula
    ADD COLUMN rb01_matricula_activa VARCHAR(64)
        GENERATED ALWAYS AS (
            IF(estado = 'ACTIVA', CONCAT(estudiante_id, '-', periodo_academico_id), NULL)
        ) STORED,
    ADD CONSTRAINT uq_matricula_activa_periodo UNIQUE (rb01_matricula_activa);

-- ---------- RB-05: un solo periodo academico activo por anio lectivo -------
-- "Solo puede existir un periodo academico activo por anio lectivo en un
-- momento dado." Los periodos inactivos quedan fuera del indice.
ALTER TABLE periodo_academico
    ADD COLUMN rb05_periodo_activo INT
        GENERATED ALWAYS AS (
            IF(activo = b'1', anio_lectivo, NULL)
        ) STORED,
    ADD CONSTRAINT uq_periodo_activo_anio UNIQUE (rb05_periodo_activo);
