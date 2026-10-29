package com.wazopulse.zoho;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ZohoExceptionHandler.class)
public class ZohoExceptionHandler {

    @ExceptionHandler(ZohoExceptions.NotConnected.class)
    ResponseEntity<Map<String, Object>> notConnected(ZohoExceptions.NotConnected e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "ZOHO_NOT_CONNECTED",
                "message", e.getMessage(),
                "reconnectRequired", e.reconnectRequired()));
    }

    @ExceptionHandler(ZohoExceptions.Api.class)
    ResponseEntity<Map<String, Object>> api(ZohoExceptions.Api e) {
        HttpStatus s = e.status() == 422 ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(s).body(Map.of("code", "ZOHO_API_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(ZohoExceptions.RateLimited.class)
    ResponseEntity<Map<String, Object>> rateLimited(ZohoExceptions.RateLimited e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(e.retryAfterSeconds()))
                .body(Map.of("code", "ZOHO_RATE_LIMITED", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "BAD_REQUEST", "message", "Invalid request body"));
    }
}
