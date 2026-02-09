package com.wmsdipl.core.web;

import com.wmsdipl.core.service.ReceiptAcceptBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {} - {}", ex.getStatusCode(), ex.getReason());
        return buildErrorResponse(ex.getStatusCode().value(), ex.getReason());
    }

    @ExceptionHandler(ReceiptAcceptBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleReceiptAcceptBlocked(ReceiptAcceptBlockedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("code", "RECEIPT_ACCEPT_BLOCKED");
        response.put("error", HttpStatus.CONFLICT.getReasonPhrase());
        response.put("message", ex.getMessage());
        response.put("receiptId", ex.getReceiptId());
        response.put("blockers", ex.getBlockers());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", errors);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("code", "VALIDATION_FAILED");
        response.put("error", "Validation Failed");
        response.put("message", "Invalid request data");
        response.put("details", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Database constraint violation", ex);
        
        String message = "Data integrity constraint violated";
        if (ex.getMessage() != null && ex.getMessage().contains("users_username_key")) {
            message = "Username already exists";
        }
        
        return buildErrorResponse(HttpStatus.CONFLICT.value(), message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied: " + ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Invalid state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.getMessage()
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(int status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status);
        response.put("code", HttpStatus.valueOf(status).name());
        response.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        response.put("message", message);
        
        return ResponseEntity.status(status).body(response);
    }
}
