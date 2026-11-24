package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentErrorHandler {

    @ExceptionHandler(PaymentRejectedException.class)
    ResponseEntity<ErrorResponse> paymentRejected(PaymentRejectedException exception) {
        HttpStatus status = switch (exception.reason()) {
            case SOLICITUD_INVALIDA, IMPORTE_INCORRECTO -> HttpStatus.BAD_REQUEST;
            case COBRO_NO_ENCONTRADO, PAGO_NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case COBRO_YA_PAGADO, COBRO_NO_PAGABLE, IDEMPOTENCIA_CONFLICTO -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(exception.reason().name(), exception.getMessage()));
    }
}
