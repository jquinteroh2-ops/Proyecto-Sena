package com.educktrack.shared.web;

import com.educktrack.shared.domain.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Traduce las excepciones de dominio y de validacion a respuestas HTTP con
 * mensajes claros en espanol (RNF-10). Centraliza el formato {@link ApiError}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    private ResponseEntity<ApiError> build(HttpStatus status, String codigo, String mensaje,
                                           List<String> detalles, HttpServletRequest req) {
        ApiError body = new ApiError(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                codigo, mensaje, detalles, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
