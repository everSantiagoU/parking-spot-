package com.parkingspot.domain.exception;

/** No existe una franja o una tarifa válida para calcular el cobro. */
public class TariffNotConfiguredException extends RuntimeException {
    public TariffNotConfiguredException(String message) {
        super(message);
    }
}
