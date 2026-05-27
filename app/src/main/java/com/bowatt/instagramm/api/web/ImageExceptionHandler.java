package com.bowatt.instagramm.api.web;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ImageExceptionHandler {

    @ExceptionHandler({InvalidApiVersionException.class, MissingApiVersionException.class})
    ResponseEntity<Map<String, String>> handleApiVersionException(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ImageUploadException.class)
    ResponseEntity<Map<String, String>> handleImageUploadException(ImageUploadException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    ResponseEntity<Map<String, String>> handleImageNotFoundException(ImageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException ex) {
        String path = ex.getResourcePath();
        String message =
                path == null || path.isBlank()
                        ? "Resource not found"
                        : "Resource not found: " + path;
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", message));
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, String>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        String supportedMethods =
                ex.getSupportedHttpMethods() == null
                        ? ""
                        : ex.getSupportedHttpMethods().stream()
                                .map(HttpMethod::name)
                                .sorted()
                                .collect(Collectors.joining(", "));
        String message =
                supportedMethods.isBlank()
                        ? "Method not allowed: " + ex.getMethod()
                        : "Method not allowed: "
                                + ex.getMethod()
                                + ". Supported methods: "
                                + supportedMethods;
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("message", message));
    }
}
