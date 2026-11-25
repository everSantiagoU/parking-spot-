package com.parkingspot.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.service.ExitValidationService;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.ExitDecision;
import com.parkingspot.domain.model.Occupancy;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.ChargeRepository;
import com.parkingspot.domain.port.out.ExitBarrier;
import com.parkingspot.domain.port.out.ExitEventRepository;
import com.parkingspot.domain.port.out.OccupancyRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.PlateClassifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * HU-21 · CP-HU-21-E02: el caso de uso de salida funciona con cualquier adaptador de barrera
 * y con cualquier repositorio que cumpla el puerto, sin modificar el servicio.
 */
class ExitBarrierAdapterTest {
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final Instant NOW = Instant.parse("2026-11-25T15:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final long SITE = 1L;

    @Test
    void abreLaBarreraDePruebaCuandoElCobroEstaPagado() {
        var barrier = new RecordingBarrier();
        var service = serviceWith(ChargeStatus.PAGADO, barrier);

        var decision = service.validate("abc123", "operador");

        assertThat(decision).isEqualTo(ExitDecision.PAGO_CONFIRMADO);
        assertThat(barrier.openings).containsExactly("1:ABC123");
    }

    @Test
    void noAbreLaBarreraDePruebaCuandoElCobroEstaPendiente() {
        var barrier = new RecordingBarrier();
        var service = serviceWith(ChargeStatus.PENDIENTE, barrier);

        var decision = service.validate("ABC123", "operador");

        assertThat(decision).isEqualTo(ExitDecision.PAGO_PENDIENTE);
        assertThat(barrier.openings).isEmpty();
    }

    private static ExitValidationService serviceWith(ChargeStatus status, ExitBarrier barrier) {
        var charge = new Charge(7L, SITE, "ABC123", VehicleType.CARRO, TODAY, TimeSlot.DIURNA, 1L,
                new BigDecimal("8000"), new BigDecimal("8000"), status, null);
        return new ExitValidationService(
                new PlateClassifier(),
                username -> Optional.of(new Operator(1L, username, SITE)),
                new OneActiveEntry(),
                new InMemoryCharges(charge),
                new OperatingDayCalculator(ZONE),
                barrier,
                Clock.fixed(NOW, ZONE),
                new FreeOccupancy(),
                new NoTransactions());
    }

    /** Adaptador de prueba: en lugar de mover una barrera, guarda a quién le abrió. */
    private static class RecordingBarrier implements ExitBarrier {
        final List<String> openings = new ArrayList<>();

        @Override
        public void open(long siteId, String plate) {
            openings.add(siteId + ":" + plate);
        }
    }

    /** Repositorio en memoria con un único cobro. */
    private record InMemoryCharges(Charge charge) implements ChargeRepository {
        @Override
        public Optional<Charge> findByPlateAndSiteAndDay(String plate, long siteId, LocalDate operatingDay) {
            return charge.plate().equals(plate) && charge.siteId() == siteId
                    && charge.operatingDay().equals(operatingDay) ? Optional.of(charge) : Optional.empty();
        }

        @Override
        public SavedCharge saveIfAbsent(Charge newCharge) {
            return new SavedCharge(charge, false);
        }
    }

    /** Ocupación en memoria: siempre hay un cupo ocupado que se puede liberar. */
    private static class FreeOccupancy implements OccupancyRepository {
        @Override
        public void lockForEntry(long siteId, VehicleType type) {
        }

        @Override
        public boolean occupySpotIfAvailable(long siteId, VehicleType type) {
            return true;
        }

        @Override
        public boolean releaseOccupiedSpot(long siteId, VehicleType type) {
            return true;
        }

        @Override
        public List<Occupancy> findBySite(long siteId) {
            return List.of();
        }
    }

    /** Sin base de datos: ejecuta el bloque directamente. */
    private static class NoTransactions implements Transactions {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    }

    /** Eventos en memoria: el vehículo tiene un ingreso activo y no audita nada. */
    private static class OneActiveEntry implements ExitEventRepository {
        @Override
        public Optional<ActiveEntry> findActiveEntry(long siteId, String plate) {
            return Optional.of(new ActiveEntry(1L, VehicleType.CARRO, null));
        }

        @Override
        public boolean recordEffectiveExit(Operator operator, ActiveEntry entry, Instant now,
                                           ExitDecision decision, Long chargeId) {
            return true;
        }

        @Override
        public void recordAttempt(Operator operator, VehicleClassification vehicle, Instant now,
                                  ExitDecision decision, Long chargeId) {
        }
    }
}
