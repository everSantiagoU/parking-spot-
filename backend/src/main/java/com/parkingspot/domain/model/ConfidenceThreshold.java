package com.parkingspot.domain.model;

/**
 * Umbral mínimo de confianza para aceptar una lectura automática sin validación del operador.
 * Una confianza menor que el umbral pide validación; una igual o mayor se acepta.
 */
public record ConfidenceThreshold(double value) {
    public ConfidenceThreshold {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "El umbral de confianza debe estar entre 0 y 1, pero se configuró " + value + ".");
        }
    }

    public boolean requiresValidation(Confidence confidence) {
        return confidence.value() < value;
    }
}
