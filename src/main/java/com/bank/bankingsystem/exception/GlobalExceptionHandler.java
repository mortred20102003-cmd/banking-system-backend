package com.bank.bankingsystem.exception;

import com.bank.bankingsystem.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 404 ----------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ---------- 409 ----------
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ---------- 400 ----------
    @ExceptionHandler({
            InvalidTransactionException.class,
            BusinessRuleException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        String msg = ex instanceof HttpMessageNotReadableException
                ? "Malformed request body"
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }

    // ---------- 400 — insufficient balance ----------
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficient(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ---------- 400 — account not active (frozen/closed) ----------
    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotActive(AccountNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

       // ---------- 401 — bad credentials ----------
    @ExceptionHandler(com.bank.bankingsystem.exception.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(
            com.bank.bankingsystem.exception.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ---------- 403 — ownership / role violation ----------
    @ExceptionHandler({UnauthorizedAccountAccessException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception ex) {
        String msg = ex instanceof UnauthorizedAccountAccessException
                ? ex.getMessage()
                : "Access denied";
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(msg));
    }

    // ---------- 400 — @Valid on DTOs ----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }

    // ---------- 400 — @Validated on params ----------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }

    // ---------- 500 — catch-all ----------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    // ---------- 404 — static resource miss (favicon, etc.) — silent ----------
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        // Do not log these at ERROR — they're harmless browser probes.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found"));
    }
}
