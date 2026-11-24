package com.parkingspot.domain;

import com.parkingspot.domain.exception.ReadingRequiresValidationException;
import com.parkingspot.domain.model.AcceptedReading;
import com.parkingspot.domain.model.Confidence;
import com.parkingspot.domain.model.ConfidenceThreshold;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.PlateReading;
import com.parkingspot.domain.model.ReadingEvaluation;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.ValidationReason;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.model.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptedReadingTest {
    private static final ConfidenceThreshold THRESHOLD = new ConfidenceThreshold(0.80);
    private static final Operator OPERATOR = new Operator(7, "operador", 1);

    private static PlateReading reading(double confidence) {
        return new PlateReading("ABC123", VehicleType.CARRO, new Confidence(confidence), ReadingSource.SIMULADOR);
    }

    @Test
    void acceptsAutomaticReadingAtOrAboveThreshold() {
        var accepted = AcceptedReading.automatic(reading(0.80), THRESHOLD, OPERATOR);

        assertEquals("ABC123", accepted.plate());
        assertEquals(ReadingSource.SIMULADOR, accepted.source());
        assertEquals(0.80, accepted.confidence().value());
        assertSame(OPERATOR, accepted.operator());
    }

    // CA-04: una lectura por debajo del umbral no puede convertirse en lectura aceptada.
    @Test
    void lowConfidenceReadingCannotBeAccepted() {
        assertThrows(ReadingRequiresValidationException.class,
                () -> AcceptedReading.automatic(reading(0.79), THRESHOLD, OPERATOR));
    }

    // CA-03: la placa digitada queda identificada como MANUAL y con el usuario que la digitó.
    @Test
    void manualReadingIsMarkedManualWithItsOperator() {
        var accepted = AcceptedReading.manual(new VehicleClassification("ABC12D", VehicleType.MOTO), OPERATOR);

        assertEquals(ReadingSource.MANUAL, accepted.source());
        assertEquals(VehicleType.MOTO, accepted.type());
        assertNull(accepted.confidence());
        assertSame(OPERATOR, accepted.operator());
    }

    @Test
    void readingThatRequiresValidationCarriesNothingToRegister() {
        var evaluation = ReadingEvaluation.requiresValidation(ValidationReason.CONFIANZA_BAJA, "ABC123", "ABC123",
                VehicleType.CARRO, new Confidence(0.5), ReadingSource.SIMULADOR);

        assertTrue(evaluation.acceptedReading().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ReadingEvaluation.requiresValidation(null, "ABC123",
                "ABC123", VehicleType.CARRO, new Confidence(0.5), ReadingSource.SIMULADOR));
    }
}
