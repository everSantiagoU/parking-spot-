package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.InvalidPlateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ClassificationErrorHandler {
    @ExceptionHandler(InvalidPlateException.class)
    ResponseEntity<ErrorResponse> invalidPlate(InvalidPlateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_PLATE", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> invalidJson() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", "La solicitud debe contener un JSON válido."));
    }

    public record ErrorResponse(String code, String message) {
    }
}
