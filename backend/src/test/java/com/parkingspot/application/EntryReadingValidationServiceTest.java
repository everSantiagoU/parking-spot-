package com.parkingspot.application;

import com.parkingspot.application.service.EntryReadingValidationService;
import com.parkingspot.domain.exception.InvalidConfidenceException;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.model.ConfidenceThreshold;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.ValidationReason;
import com.parkingspot.domain.model.ValidationStatus;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.service.PlateClassifier;
import com.parkingspot.infrastructure.adapter.out.plate.SimulatedPlateReader;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryReadingValidationServiceTest {
    private static final Operator OPERATOR = new Operator(7, "operador", 1);

    private final EntryReadingValidationService service = new EntryReadingValidationService(
            new SimulatedPlateReader(), new PlateClassifier(), new ConfidenceThreshold(0.80),
            username -> "operador".equals(username) ? Optional.of(OPERATOR) : Optional.empty());

    @Test
    void acceptsReadingAtOrAboveThreshold() {
        var evaluation = service.validateAutomatic(new PlateCapture(" abc123 ", 0.80), "operador");

        assertEquals(ValidationStatus.ACEPTADA, evaluation.status());
        assertNull(evaluation.reason());
        assertEquals("ABC123", evaluation.plate());
        var accepted = evaluation.acceptedReading().orElseThrow();
        assertEquals(ReadingSource.SIMULADOR, accepted.source());
        assertEquals(OPERATOR, accepted.operator());
    }

    // CA-02 y CA-04: debajo del umbral pide validación y no hay lectura aceptada que registrar.
    @Test
    void lowConfidenceRequiresValidation() {
        var evaluation = service.validateAutomatic(new PlateCapture("ABC123", 0.79), "operador");

        assertEquals(ValidationStatus.REQUIERE_VALIDACION, evaluation.status());
        assertEquals(ValidationReason.CONFIANZA_BAJA, evaluation.reason());
        assertEquals("ABC123", evaluation.plate());
        assertEquals(VehicleType.CARRO, evaluation.type());
        assertTrue(evaluation.acceptedReading().isEmpty());
    }

    // Formato inválido → requiere validación, sin importar la confianza.
    @Test
    void invalidFormatRequiresValidationEvenWithHighConfidence() {
        var evaluation = service.validateAutomatic(new PlateCapture("AB123", 0.99), "operador");

        assertEquals(ValidationStatus.REQUIERE_VALIDACION, evaluation.status());
        assertEquals(ValidationReason.FORMATO_INVALIDO, evaluation.reason());
        assertEquals("AB123", evaluation.readText());
        assertNull(evaluation.plate());
        assertTrue(evaluation.acceptedReading().isEmpty());
    }

    @Test
    void invalidConfidenceIsStillRejected() {
        assertThrows(InvalidConfidenceException.class,
                () -> service.validateAutomatic(new PlateCapture("ABC123", 1.5), "operador"));
    }

    // CA-03: la placa confirmada queda MANUAL y con el operador que la digitó.
    @Test
    void manualConfirmationIsMarkedManualWithOperator() {
        var accepted = service.confirmManual(" abc12d ", "operador");

        assertEquals("ABC12D", accepted.plate());
        assertEquals(VehicleType.MOTO, accepted.type());
        assertEquals(ReadingSource.MANUAL, accepted.source());
        assertEquals(OPERATOR, accepted.operator());
    }

    @Test
    void manualConfirmationRejectsInvalidPlate() {
        assertThrows(InvalidPlateException.class, () -> service.confirmManual("AB123", "operador"));
    }

    @Test
    void unknownOperatorCannotValidateOrConfirm() {
        assertThrows(OperatorNotAvailableException.class,
                () -> service.validateAutomatic(new PlateCapture("ABC123", 0.9), "otro"));
        assertThrows(OperatorNotAvailableException.class, () -> service.confirmManual("ABC123", "otro"));
    }
}
