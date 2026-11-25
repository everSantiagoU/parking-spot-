package com.parkingspot.domain.model;

import java.util.Objects;

/**
 * Lectura de placa ya normalizada y clasificada, lista para los siguientes pasos del ingreso.
 */
public record PlateReading(String plate, VehicleType type, Confidence confidence, ReadingSource source) {
    public PlateReading {
        Objects.requireNonNull(plate, "plate");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(source, "source");
    }
}
