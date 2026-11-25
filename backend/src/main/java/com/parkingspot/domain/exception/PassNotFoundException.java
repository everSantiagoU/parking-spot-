package com.parkingspot.domain.exception;

/** La placa no tiene ningún cobro pagado, así que no tiene pase. */
public class PassNotFoundException extends RuntimeException {
    public PassNotFoundException(String plate) {
        super("La placa " + plate + " no tiene un pase registrado.");
    }
}
