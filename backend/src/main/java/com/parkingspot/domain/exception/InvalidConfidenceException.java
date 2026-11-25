package com.parkingspot.domain.exception;

public class InvalidConfidenceException extends RuntimeException {
    public InvalidConfidenceException() {
        super("La confianza debe ser un número entre 0 y 1.");
    }
}
