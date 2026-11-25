package com.parkingspot.infrastructure;

import com.parkingspot.domain.exception.InvalidConfidenceException;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.infrastructure.adapter.out.plate.SimulatedPlateReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulatedPlateReaderTest {
    private final SimulatedPlateReader reader = new SimulatedPlateReader();

    @Test
    void returnsReceivedPlateAndConfidenceWithSimulatorSource() {
        var raw = reader.read(new PlateCapture(" abc123 ", 0.75));

        // El lector no normaliza: eso lo hace PlateClassifier en la aplicación.
        assertEquals(" abc123 ", raw.plateText());
        assertEquals(0.75, raw.confidence().value());
        assertEquals(ReadingSource.SIMULADOR, raw.source());
    }

    @Test
    void rejectsOutOfRangeOrMissingConfidence() {
        assertThrows(InvalidConfidenceException.class, () -> reader.read(new PlateCapture("ABC123", 1.5)));
        assertThrows(InvalidConfidenceException.class, () -> reader.read(new PlateCapture("ABC123", null)));
    }
}
