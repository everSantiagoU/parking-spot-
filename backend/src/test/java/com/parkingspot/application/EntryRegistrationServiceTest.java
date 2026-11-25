package com.parkingspot.application;

import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.service.EntryReadingValidationService;
import com.parkingspot.application.service.EntryRegistrationService;
import com.parkingspot.domain.exception.OccupancyNotConfiguredException;
import com.parkingspot.domain.model.ConfidenceThreshold;
import com.parkingspot.domain.model.EntryEvent;
import com.parkingspot.domain.model.EntryOutcome;
import com.parkingspot.domain.model.EntryResult;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.EntryEventRepository;
import com.parkingspot.domain.port.out.OccupancyRepository;
import com.parkingspot.domain.service.PlateClassifier;
import com.parkingspot.infrastructure.adapter.out.plate.SimulatedPlateReader;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryRegistrationServiceTest {
    private static final Operator OPERATOR = new Operator(7, "operador", 3);
    private static final Instant NOW = Instant.parse("2026-11-24T13:45:00Z");

    private final List<EntryEvent> savedEvents = new ArrayList<>();
    private final List<String> occupiedSpots = new ArrayList<>();
    private final EntryEventRepository events = event -> {
        savedEvents.add(event);
        return savedEvents.size();
    };
    private final OccupancyRepository occupancy = (siteId, type) -> occupiedSpots.add(siteId + ":" + type);

    private EntryRegistrationService service(OccupancyRepository occupancyRepository) {
        var validation = new EntryReadingValidationService(new SimulatedPlateReader(), new PlateClassifier(),
                new ConfidenceThreshold(0.80), username -> Optional.of(OPERATOR));
        // Clock fijo: la prueba controla "la hora del servidor".
        return new EntryRegistrationService(validation, events, occupancyRepository, new DirectTransactions(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** Sin base de datos: ejecuta el bloque directamente. */
    private static final class DirectTransactions implements Transactions {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    }

    @Test
    void registersAcceptedAutomaticReadingWithServerTimeAndOccupiesSpot() {
        var attempt = service(occupancy).enterAutomatic(new PlateCapture(" abc123 ", 0.93), "operador");

        assertEquals(EntryOutcome.AUTORIZADO, attempt.outcome());
        assertTrue(attempt.barrierOpen());
        assertEquals(1L, attempt.eventId());
        var event = savedEvents.get(0);
        assertEquals("ABC123", event.plate());
        assertEquals(3, event.siteId());
        assertEquals(ReadingSource.SIMULADOR, event.source());
        assertEquals(EntryResult.AUTORIZADO, event.result());
        assertEquals(NOW, event.occurredAt());
        assertEquals(List.of("3:CARRO"), occupiedSpots);
    }

    // CA-04 de la HU-11: una lectura que requiere validación no registra nada.
    @Test
    void readingThatRequiresValidationRegistersNothing() {
        var attempt = service(occupancy).enterAutomatic(new PlateCapture("ABC123", 0.5), "operador");

        assertEquals(EntryOutcome.REQUIERE_VALIDACION, attempt.outcome());
        assertEquals("CONFIANZA_BAJA", attempt.reason());
        assertFalse(attempt.barrierOpen());
        assertNull(attempt.eventId());
        assertTrue(savedEvents.isEmpty());
        assertTrue(occupiedSpots.isEmpty());
    }

    @Test
    void registersManualReadingWithOperatorAndNoConfidence() {
        var attempt = service(occupancy).enterManual("abc12d", "operador");

        assertEquals(EntryOutcome.AUTORIZADO, attempt.outcome());
        var event = savedEvents.get(0);
        assertEquals(ReadingSource.MANUAL, event.source());
        assertEquals(VehicleType.MOTO, event.type());
        assertNull(event.confidence());
        assertEquals(7, event.userId());
    }

    // HU-13: sin cupo → LLENO, barrera cerrada y el intento queda guardado como RECHAZADO.
    @Test
    void fullSiteRejectsEntryKeepsBarrierClosedAndStoresRejectedEvent() {
        OccupancyRepository full = (siteId, type) -> false;

        var attempt = service(full).enterAutomatic(new PlateCapture("ABC123", 0.93), "operador");

        assertEquals(EntryOutcome.LLENO, attempt.outcome());
        assertFalse(attempt.barrierOpen());
        assertEquals("LLENO", attempt.reason());
        assertEquals(1L, attempt.eventId());
        var event = savedEvents.get(0);
        assertEquals(EntryResult.RECHAZADO, event.result());
        assertEquals("LLENO", event.reason());
        assertEquals(NOW, event.occurredAt());
    }

    @Test
    void doesNotSaveEventWhenOccupancyIsNotConfigured() {
        OccupancyRepository missing = (siteId, type) -> {
            throw new OccupancyNotConfiguredException(siteId, type);
        };

        assertThrows(OccupancyNotConfiguredException.class,
                () -> service(missing).enterAutomatic(new PlateCapture("ABC123", 0.9), "operador"));
        assertTrue(savedEvents.isEmpty());
    }
}
