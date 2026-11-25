package com.parkingspot.domain.exception;

public class ReadingRequiresValidationException extends RuntimeException {
    public ReadingRequiresValidationException() {
        super("La lectura requiere que el operador digite o confirme la placa antes del ingreso.");
    }
}
