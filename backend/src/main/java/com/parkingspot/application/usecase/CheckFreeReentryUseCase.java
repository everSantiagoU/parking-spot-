package com.parkingspot.application.usecase;

/**
 * Responde si una placa puede reingresar sin cobro (HU-17 CA-04 y CA-05).
 * Es el punto de integración para el registro de ingresos (HU-10).
 */
public interface CheckFreeReentryUseCase {
    boolean hasValidPass(String plate, long siteId);
}
