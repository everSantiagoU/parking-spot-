package com.parkingspot.domain.model;

import com.parkingspot.domain.exception.InvalidConfidenceException;

/**
 * Confianza de una lectura de placa: un número entre 0 y 1, ambos incluidos.
 * La regla vive en el dominio para que ningún lector pueda producir un valor inválido.
 */
public record Confidence(double value) {
    public Confidence {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new InvalidConfidenceException();
        }
    }

    /** Crea la confianza a partir de un valor que puede venir ausente (por ejemplo, desde JSON). */
    public static Confidence of(Double value) {
        if (value == null) {
            throw new InvalidConfidenceException();
        }
        return new Confidence(value);
    }
}
