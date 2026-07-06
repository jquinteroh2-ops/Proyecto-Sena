package com.educktrack.shared.web;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cuerpo estandar de una respuesta de error de la API. Los mensajes van en
 * espanol e indican causa (RNF-10).
 *
 * @param timestamp  momento del error
 * @param status     codigo HTTP
 * @param error      nombre del estado HTTP
 * @param codigo     identificador de regla/negocio cuando aplica (p. ej. "RB-14"), o null
 * @param mensaje    descripcion legible en espanol
 * @param detalles   errores de validacion por campo, cuando aplica
 * @param path       ruta solicitada
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String codigo,
        String mensaje,
        List<String> detalles,
        String path) {
}
