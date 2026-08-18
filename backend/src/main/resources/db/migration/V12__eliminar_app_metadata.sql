-- ==========================================================================
-- V12 - Eliminar la tabla app_metadata
--
-- Creada en V1 "por si acaso": nunca se mapeo a una entidad JPA, ningun
-- servicio la lee y ninguno la escribe. Su contenido son las dos filas
-- literales que insertaba la propia V1 ('schema_version' = 'baseline' y
-- 'proyecto' = 'EduckTrack'), de modo que no hay dato que conservar: la
-- version real del esquema la lleva flyway_schema_history, que es la unica
-- fuente de verdad del esquema (RS-02).
--
-- Se elimina por la regla del proyecto de no mantener tablas sin uso: una
-- tabla vacia de proposito invita a que alguien le encuentre uno distinto del
-- que tenia, y a partir de ahi el esquema deja de explicarse solo.
-- ==========================================================================

DROP TABLE IF EXISTS app_metadata;
