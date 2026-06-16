package com.vortexmesh.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleServiceNotFound(ServiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "error", "SERVICE_NOT_FOUND",
            "message", ex.getMessage(),
            "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
            "error", "RATE_LIMIT_EXCEEDED",
            "message", ex.getMessage(),
            "retryAfter", ex.getRetryAfterSeconds(),
            "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<Map<String, Object>> handlePolicyViolation(PolicyViolationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "POLICY_VIOLATION",
            "message", ex.getMessage(),
            "timestamp", Instant.now().toString()
        ));
    }
}
