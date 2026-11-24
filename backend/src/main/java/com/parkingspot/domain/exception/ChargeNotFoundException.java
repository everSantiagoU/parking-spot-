package com.parkingspot.domain.exception;

/** La placa no tiene cobro en el día operativo consultado. */
public class ChargeNotFoundException extends RuntimeException {
    public ChargeNotFoundException(String plate) {
        super("La placa " + plate + " no tiene cobro en el día operativo actual.");
    }
}
