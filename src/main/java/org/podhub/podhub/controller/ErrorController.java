package org.podhub.podhub.controller;

import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.ErrorResponse;
import org.podhub.podhub.exception.BadRequestException;
import org.podhub.podhub.exception.ConflictException;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorController {

    // 404 - Recurso no encontrado en vuestros servicios
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("404 Not Found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        req.getRequestURI(),
                        null
                ));
    }

    // 409 - Índices únicos de Mongo: email/username, slug, (userId, podcastId), (userId, episodeId)...
    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class, ConflictException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest req) {
        log.warn("=== 409 CONFLICT HANDLER TRIGGERED ===");
        log.warn("Exception type: {}", ex.getClass().getName());
        log.warn("Exception message: {}", ex.getMessage());
        log.warn("Full stack trace:", ex);
        log.warn("Request URI: {}", req.getRequestURI());
        log.warn("Request Method: {}", req.getMethod());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        "Duplicate key / conflict: " + ex.getMessage(),
                        req.getRequestURI(),
                        null
                ));
    }

    // 400 - Validación de DTOs con @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.debug("400 Validation errors: {}", errors);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Validation failed",
                        req.getRequestURI(),
                        errors
                ));
    }

    // 400 - JSON mal formado / tipos incorrectos
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.debug("400 Unreadable body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Malformed JSON request",
                        req.getRequestURI(),
                        null
                ));
    }

    // 400 - Reglas de dominio que invalidan la operación
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        log.debug("400 Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ex.getMessage(),
                        req.getRequestURI(),
                        null
                ));
    }

    // 500 - Cualquier otra cosa que se escape
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("500 Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "Unexpected error",
                        req.getRequestURI(),
                        null
                ));
    }
}
