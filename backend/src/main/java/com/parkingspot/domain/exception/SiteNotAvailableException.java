package com.parkingspot.domain.exception;

/** La sede no existe o no está activa. */
public class SiteNotAvailableException extends RuntimeException {
    public SiteNotAvailableException() {
        super("Sede no disponible.");
    }
}
