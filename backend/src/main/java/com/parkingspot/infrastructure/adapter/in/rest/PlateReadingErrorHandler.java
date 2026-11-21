package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.InvalidConfidenceException;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores del lector de placas a HTTP. Reutiliza ErrorResponse para mantener
 * el mismo formato {code, message}. INVALID_PLATE e INVALID_REQUEST los sigue
 * respondiendo ClassificationErrorHandler.
 */
@RestControllerAdvice
public class PlateReadingErrorHandler {
    @ExceptionHandler(InvalidConfidenceException.class)
    ResponseEntity<ErrorResponse> invalidConfidence(InvalidConfidenceException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_CONFIDENCE", exception.getMessage()));
    }
}
