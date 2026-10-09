package com.educktrack.shared.web;

import com.educktrack.shared.domain.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Traduce las excepciones de dominio y de validacion a respuestas HTTP con
 * mensajes claros en espanol (RNF-10). Centraliza el formato {@link ApiError}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Violacion de una regla de negocio (RB-xx) -> 409 Conflict. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> handleReglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getCodigoRegla(), ex.getMessage(), null, req);
    }

    /** Datos de entrada invalidos (Bean Validation) -> 400 con detalle por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, null,
                "Existen errores de validacion en los datos enviados.", detalles, req);
    }

    /** Credenciales o token invalidos -> 401. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, null,
                "Credenciales invalidas o sesion no autenticada.", null, req);
    }

    /** Falta de permisos para el rol autenticado (RS-03) -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, null,
                "No tiene permisos para realizar esta accion.", null, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, null, ex.getMessage(), null, req);
    }

    /**
     * Peticion mal formada: falta un parametro obligatorio, un valor no tiene
     * el tipo esperado (texto donde va un numero) o el JSON no se puede leer.
     * Antes caian en /error, que exige sesion, y el cliente recibia un 401
     * "Debe autenticarse" que no tenia nada que ver con el problema.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleParametroFaltante(MissingServletRequestParameterException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, null,
                "Falta el parametro obligatorio '" + ex.getParameterName() + "'.", null, req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, null,
                "El valor del parametro '" + ex.getName() + "' no es valido.", null, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleCuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, null,
                "El cuerpo de la peticion no es un JSON valido o tiene datos con formato incorrecto.", null, req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMetodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, null,
                "El metodo " + ex.getMethod() + " no esta permitido en esta ruta.", null, req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleRutaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, null, "La ruta solicitada no existe.", null, req);
    }

    /** Cualquier otro error: 500 con mensaje generico; el detalle queda en el log, no en la respuesta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInesperado(Exception ex, HttpServletRequest req) {
        // Las excepciones que ya traen su codigo HTTP (ResponseStatusException
        // y demas ErrorResponse de Spring) lo conservan.
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return build(status, null, er.getBody().getDetail() != null
                    ? er.getBody().getDetail() : status.getReasonPhrase(), null, req);
        }
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, null,
                "Ocurrio un error inesperado. Intente de nuevo mas tarde.", null, req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String codigo, String mensaje,
                                           List<String> detalles, HttpServletRequest req) {
        ApiError body = new ApiError(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                codigo, mensaje, detalles, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
