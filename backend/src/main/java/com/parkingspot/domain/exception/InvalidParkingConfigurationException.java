package com.parkingspot.domain.exception;

/** La configuración de tarifas, franjas o cupos de una sede no cumple las reglas del negocio (HU-12). */
public class InvalidParkingConfigurationException extends RuntimeException {
    public InvalidParkingConfigurationException(String message) {
        super(message);
    }
}
