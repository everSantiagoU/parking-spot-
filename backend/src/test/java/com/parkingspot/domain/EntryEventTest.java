package com.parkingspot.domain;

import com.parkingspot.domain.model.AcceptedReading;
import com.parkingspot.domain.model.Confidence;
import com.parkingspot.domain.model.ConfidenceThreshold;
import com.parkingspot.domain.model.EntryEvent;
import com.parkingspot.domain.model.EntryResult;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.PlateReading;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.model.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntryEventTest {
    private static final Operator OPERATOR = new Operator(7, "operador", 3);
    private static final Instant NOW = Instant.parse("2026-11-24T13:45:00Z");

    // CA-01, CA-02 y CA-04: placa, sede del operador, hora recibida y origen de la lectura.
    @Test
    void authorizedEventKeepsPlateSiteTimeSourceAndUser() {
        var reading = new PlateReading("ABC123", VehicleType.CARRO, new Confidence(0.93), ReadingSource.SIMULADOR);
        var event = EntryEvent.authorized(
                AcceptedReading.automatic(reading, new ConfidenceThreshold(0.8), OPERATOR), NOW);

        assertEquals("ABC123", event.plate());
        assertEquals(3, event.siteId());
        assertEquals(VehicleType.CARRO, event.type());
        assertEquals(ReadingSource.SIMULADOR, event.source());
        assertEquals(0.93, event.confidence().value());
        assertEquals(EntryResult.AUTORIZADO, event.result());
        assertNull(event.reason());
        assertEquals(7, event.userId());
        assertEquals(NOW, event.occurredAt());
    }

    @Test
    void manualEventHasNoConfidenceAndKeepsItsUser() {
        var event = EntryEvent.authorized(
                AcceptedReading.manual(new VehicleClassification("ABC12D", VehicleType.MOTO), OPERATOR), NOW);

        assertEquals(ReadingSource.MANUAL, event.source());
        assertNull(event.confidence());
        assertEquals(7, event.userId());
    }

    @Test
    void manualEventCannotCarryReaderConfidence() {
        assertThrows(IllegalArgumentException.class, () -> new EntryEvent(3, "ABC123", VehicleType.CARRO,
                ReadingSource.MANUAL, new Confidence(0.5), EntryResult.AUTORIZADO, null, 7, NOW));
    }
}
