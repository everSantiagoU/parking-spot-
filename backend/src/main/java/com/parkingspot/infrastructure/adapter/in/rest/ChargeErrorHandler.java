package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.ChargeNotFoundException;
import com.parkingspot.domain.exception.TariffNotConfiguredException;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChargeErrorHandler {

    @ExceptionHandler(TariffNotConfiguredException.class)
    ResponseEntity<ErrorResponse> tariffNotConfigured(TariffNotConfiguredException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("TARIFA_NO_CONFIGURADA", exception.getMessage()));
    }

    @ExceptionHandler(ChargeNotFoundException.class)
    ResponseEntity<ErrorResponse> chargeNotFound(ChargeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("COBRO_NO_ENCONTRADO", exception.getMessage()));
    }

    @ExceptionHandler(ChargeController.OperatorWithoutSiteException.class)
    ResponseEntity<ErrorResponse> operatorWithoutSite(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("OPERADOR_SIN_SEDE", exception.getMessage()));
    }
}
