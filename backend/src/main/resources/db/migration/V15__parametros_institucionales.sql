-- ==========================================================================
-- V15 - Parametros institucionales configurables (Fase 9)
-- RF-59 (el Rector define la escala de calificacion y el porcentaje minimo de
-- asistencia institucional), RS-14, RB-03, RB-04, RB-09.
--
-- Hasta ahora estos valores eran constantes de codigo (Calificacion.NOTA_*,
-- AsistenciaService.PORCENTAJE_MINIMO) o propiedades del despliegue
-- (educktrack.academico.max-horas-docente). Ninguna de las dos cosas cumple
-- RF-59: el requisito pide que los defina el Rector desde el sistema, y hoy
-- cambiarlos exige recompilar o reiniciar.
--
-- Tabla clave/valor y no una columna por parametro: el conjunto de parametros
-- crece con el tiempo y una tabla de una sola fila con N columnas obliga a una
-- migracion por cada uno. El valor se guarda como texto y lo interpreta el
-- servicio, porque los tipos son distintos (decimal, porcentaje, entero) y una
-- columna por tipo seria peor que convertir en un solo sitio.
--
-- Las claves validas son un enum cerrado en el codigo (ParametroInstitucional),
-- de modo que anadir un parametro sea una decision explicita y no una fila
-- suelta que nadie lee. Por eso no hay tabla de claves permitidas.
--
-- Se siembran los valores que hoy estan en el codigo, para que desplegar esta
-- migracion NO cambie el comportamiento de nada.
-- ==========================================================================

CREATE TABLE parametro_institucional (
    clave           VARCHAR(60)  NOT NULL PRIMARY KEY,
    valor           VARCHAR(100) NOT NULL,
    actualizado     DATETIME(6)  NOT NULL,
    -- Correo de quien lo cambio, en texto: mismo criterio que el log de
    -- auditoria (V11), el registro debe sobrevivir al borrado de la cuenta.
    actualizado_por VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO parametro_institucional (clave, valor, actualizado, actualizado_por) VALUES
    ('NOTA_MINIMA',                '1.00', NOW(6), 'sistema'),
    ('NOTA_MAXIMA',                '5.00', NOW(6), 'sistema'),
    ('NOTA_APROBATORIA',           '3.00', NOW(6), 'sistema'),
    ('PORCENTAJE_MINIMO_ASISTENCIA', '80',  NOW(6), 'sistema'),
    ('MAX_HORAS_DOCENTE',            '30',  NOW(6), 'sistema');
