package com.parkingspot.domain.exception;

public class InvalidPlateException extends RuntimeException {
    public InvalidPlateException() {
        super("La placa debe tener el formato ABC123 (carro) o ABC12D (moto).");
    }
}
