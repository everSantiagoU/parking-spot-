package com.parkingspot.domain;

import com.parkingspot.domain.model.Confidence;
import com.parkingspot.domain.model.ConfidenceThreshold;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfidenceThresholdTest {
    private final ConfidenceThreshold threshold = new ConfidenceThreshold(0.80);

    // CA-02: una confianza menor al umbral pide validación.
    @Test
    void lowerConfidenceRequiresValidation() {
        assertTrue(threshold.requiresValidation(new Confidence(0.79)));
        assertTrue(threshold.requiresValidation(new Confidence(0.0)));
    }

    // Frontera: igual al umbral NO pide validación ("menor al umbral").
    @ParameterizedTest
    @ValueSource(doubles = {0.80, 0.81, 1.0})
    void equalOrHigherConfidenceIsAccepted(double value) {
        assertFalse(threshold.requiresValidation(new Confidence(value)));
    }

    // CA-01: el umbral configurable también debe ser un valor entre 0 y 1.
    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.5, Double.NaN})
    void rejectsThresholdOutsideRange(double value) {
        assertThrows(IllegalArgumentException.class, () -> new ConfidenceThreshold(value));
    }
}
