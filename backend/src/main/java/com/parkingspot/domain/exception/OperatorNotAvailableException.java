package com.parkingspot.domain.exception;

public class OperatorNotAvailableException extends RuntimeException {
    public OperatorNotAvailableException() {
        super("El operador no está activo o no tiene una sede asignada.");
    }
}
