package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.PassNotFoundException;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PassErrorHandler {

    @ExceptionHandler(PassNotFoundException.class)
    ResponseEntity<ErrorResponse> passNotFound(PassNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PASE_NO_ENCONTRADO", exception.getMessage()));
    }
}
