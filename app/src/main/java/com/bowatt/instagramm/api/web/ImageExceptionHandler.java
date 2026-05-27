package com.bowatt.instagramm.api.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ImageExceptionHandler {

    @ExceptionHandler(ImageUploadException.class)
    ResponseEntity<Map<String, String>> handleImageUploadException(ImageUploadException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    ResponseEntity<Map<String, String>> handleImageNotFoundException(ImageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }
}
