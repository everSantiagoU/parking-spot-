package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del flujo de ingreso, con el mismo formato {code, message}. */
@RestControllerAdvice
public class EntryErrorHandler {
    @ExceptionHandler(OperatorNotAvailableException.class)
    ResponseEntity<ErrorResponse> operatorNotAvailable(OperatorNotAvailableException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("OPERATOR_NOT_AVAILABLE", exception.getMessage()));
    }
}
