-- ==========================================================================
-- V14 - Las calificaciones pasan de DOUBLE a DECIMAL(3,2) (Fase 8)
-- RB-03 (escala 1.0 a 5.0) y RB-12 (aprueba con promedio >= 3.0).
--
-- DOUBLE es binario y no puede representar exactamente valores como 2.9 o
-- 3.05. Con notas eso no es teorico: RB-12 decide la aprobacion comparando
-- contra 3.0, y una nota guardada como 2.9999999999999996 esta del lado
-- equivocado de una comparacion que decide si alguien pierde el ano. El error
-- ademas se acumula al promediar varias notas ponderadas.
--
-- DECIMAL(3,2) cubre la escala completa (1.00 a 5.00) con dos decimales
-- exactos, que es la precision con la que se registran y se muestran las
-- notas. La conversion es segura: los valores existentes ya estan en ese
-- rango, y MySQL redondea a dos decimales al cambiar el tipo.
--
-- Se convierten las tres columnas que guardan una nota en la escala RB-03. El
-- porcentaje de asistencia (RB-04) se queda como esta: es un porcentaje
-- calculado, no un valor de la escala, y ahi el redondeo no decide nada.
-- ==========================================================================

ALTER TABLE calificacion
    MODIFY COLUMN valor DECIMAL(3,2) NOT NULL;

ALTER TABLE novedad_nota
    MODIFY COLUMN valor_anterior DECIMAL(3,2) NOT NULL,
    MODIFY COLUMN valor_nuevo    DECIMAL(3,2) NOT NULL;

ALTER TABLE entrega_tarea
    MODIFY COLUMN calificacion DECIMAL(3,2) NULL;
