package com.parkingspot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.application.dto.ChargeResult;
import com.parkingspot.application.dto.ChargeResult.Outcome;
import com.parkingspot.application.service.ChargeService;
import com.parkingspot.domain.exception.ChargeNotFoundException;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.exception.TariffNotConfiguredException;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.TimeSlotSchedule;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.ChargeRepository;
import com.parkingspot.domain.port.out.TariffRepository;
import com.parkingspot.domain.port.out.TimeSlotRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.PlateClassifier;
import com.parkingspot.domain.service.TimeSlotResolver;
import com.parkingspot.domain.service.tariff.CarTariffStrategy;
import com.parkingspot.domain.service.tariff.MotorcycleTariffStrategy;
import com.parkingspot.domain.service.tariff.TariffStrategyResolver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del motor de cobros (HU-14) y del día operativo y la unicidad (HU-15). */
class ChargeServiceTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final long SITE = 1L;

    private final Map<String, Tariff> tariffs = new HashMap<>();
    private final InMemoryCharges charges = new InMemoryCharges();

    @BeforeEach
    void tarifasIniciales() {
        tariffs.put("CARRO|DIURNA", new Tariff(1, VehicleType.CARRO, TimeSlot.DIURNA, new BigDecimal("10000")));
        tariffs.put("CARRO|NOCTURNA", new Tariff(2, VehicleType.CARRO, TimeSlot.NOCTURNA, new BigDecimal("6000")));
        tariffs.put("MOTO|DIURNA", new Tariff(3, VehicleType.MOTO, TimeSlot.DIURNA, BigDecimal.ZERO));
        tariffs.put("MOTO|NOCTURNA", new Tariff(4, VehicleType.MOTO, TimeSlot.NOCTURNA, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("CP-HU-14-E01 / CA-02: carro en franja diurna genera cobro PENDIENTE con la tarifa diurna")
    void carroDiurno() {
        ChargeResult result = serviceAt("2026-11-23T10:00").generate(" abc123 ", SITE);

        assertThat(result.outcome()).isEqualTo(Outcome.CREADO);
        assertThat(result.plate()).isEqualTo("ABC123");
        assertThat(result.timeSlot()).isEqualTo(TimeSlot.DIURNA);
        assertThat(result.operatingDay()).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(result.charge().amount()).isEqualByComparingTo("10000");
        assertThat(result.charge().appliedTariff()).isEqualByComparingTo("10000");
        assertThat(result.charge().status()).isEqualTo(ChargeStatus.PENDIENTE);
    }

    @Test
    @DisplayName("CA-03: carro en franja nocturna aplica la tarifa nocturna")
    void carroNocturno() {
        ChargeResult result = serviceAt("2026-11-23T20:00").generate("XYZ789", SITE);

        assertThat(result.timeSlot()).isEqualTo(TimeSlot.NOCTURNA);
        assertThat(result.charge().amount()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("CP-HU-14-E02 / CA-01: moto con tarifa $0 no genera obligación de pago")
    void motoExenta() {
        ChargeResult result = serviceAt("2026-11-23T10:00").generate("ABC12D", SITE);

        assertThat(result.outcome()).isEqualTo(Outcome.EXENTO);
        assertThat(result.charge()).isNull();
        assertThat(charges.count()).isZero();
    }

    @Test
    @DisplayName("HU-15 CA-03: el reingreso en el mismo día operativo no crea otro cobro")
    void reingresoMismoDia() {
        ChargeResult first = serviceAt("2026-11-23T10:00").generate("ABC123", SITE);
        ChargeResult again = serviceAt("2026-11-23T15:00").generate("ABC123", SITE);
        ChargeResult earlyMorning = serviceAt("2026-11-24T03:00").generate("ABC123", SITE);

        assertThat(again.outcome()).isEqualTo(Outcome.EXISTENTE);
        assertThat(earlyMorning.outcome()).isEqualTo(Outcome.EXISTENTE);
        assertThat(again.charge().id()).isEqualTo(first.charge().id());
        assertThat(charges.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-15 CA-04 / CP-HU-15-E02: en un nuevo día operativo se genera un nuevo cobro")
    void nuevoDiaOperativo() {
        ChargeResult first = serviceAt("2026-11-23T10:00").generate("ABC123", SITE);
        ChargeResult nextDay = serviceAt("2026-11-24T06:00").generate("ABC123", SITE);

        assertThat(nextDay.outcome()).isEqualTo(Outcome.CREADO);
        assertThat(nextDay.charge().id()).isNotEqualTo(first.charge().id());
        assertThat(nextDay.operatingDay()).isEqualTo(LocalDate.of(2026, 11, 24));
    }

    @Test
    @DisplayName("HU-14 CA-04: cambiar la tarifa no altera un cobro ya generado")
    void tarifaConservada() {
        serviceAt("2026-11-23T10:00").generate("ABC123", SITE);
        tariffs.put("CARRO|DIURNA", new Tariff(1, VehicleType.CARRO, TimeSlot.DIURNA, new BigDecimal("12000")));

        Charge charge = serviceAt("2026-11-23T11:00").findToday("ABC123", SITE);

        assertThat(charge.amount()).isEqualByComparingTo("10000");
        assertThat(charge.appliedTariff()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("CP-HU-14-X01 / CA-06: sin tarifa aplicable hay error controlado y no se registra nada")
    void sinTarifa() {
        tariffs.remove("CARRO|DIURNA");

        assertThatThrownBy(() -> serviceAt("2026-11-23T10:00").generate("ABC123", SITE))
                .isInstanceOf(TariffNotConfiguredException.class);
        assertThat(charges.count()).isZero();
    }

    @Test
    void placaInvalida() {
        assertThatThrownBy(() -> serviceAt("2026-11-23T10:00").generate("12", SITE))
                .isInstanceOf(InvalidPlateException.class);
    }

    @Test
    void consultaSinCobro() {
        assertThatThrownBy(() -> serviceAt("2026-11-23T10:00").findToday("ZZZ999", SITE))
                .isInstanceOf(ChargeNotFoundException.class);
    }

    @Test
    @DisplayName("HU-15 CA-06: con solicitudes simultáneas existe como máximo un cobro")
    void solicitudesSimultaneas() throws Exception {
        ChargeService service = serviceAt("2026-11-23T10:00");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ChargeResult>> results = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return service.generate("RAC123", SITE);
            }));
        }
        start.countDown();
        int created = 0;
        for (Future<ChargeResult> result : results) {
            if (result.get().outcome() == Outcome.CREADO) {
                created++;
            }
        }
        pool.shutdown();

        assertThat(created).isEqualTo(1);
        assertThat(charges.count()).isEqualTo(1);
    }

    private ChargeService serviceAt(String localDateTime) {
        Clock clock = Clock.fixed(LocalDateTime.parse(localDateTime).atZone(BOGOTA).toInstant(), BOGOTA);
        TimeSlotRepository timeSlots = site -> List.of(
                new TimeSlotSchedule(TimeSlot.DIURNA, LocalTime.of(6, 0), LocalTime.of(18, 0)),
                new TimeSlotSchedule(TimeSlot.NOCTURNA, LocalTime.of(18, 0), LocalTime.of(6, 0)));
        TariffRepository tariffRepository = (site, type, slot) -> Optional.ofNullable(tariffs.get(type + "|" + slot));
        return new ChargeService(new PlateClassifier(), new OperatingDayCalculator(BOGOTA), new TimeSlotResolver(),
                new TariffStrategyResolver(List.of(new CarTariffStrategy(), new MotorcycleTariffStrategy())),
                timeSlots, tariffRepository, charges, clock);
    }

    /** Repositorio en memoria que imita la restricción única de PostgreSQL. */
    private static class InMemoryCharges implements ChargeRepository {
        private final Map<String, Charge> rows = new HashMap<>();
        private final AtomicLong ids = new AtomicLong();

        @Override
        public synchronized Optional<Charge> findByPlateAndSiteAndDay(String plate, long siteId, LocalDate day) {
            return Optional.ofNullable(rows.get(key(plate, siteId, day)));
        }

        @Override
        public synchronized SavedCharge saveIfAbsent(Charge charge) {
            String key = key(charge.plate(), charge.siteId(), charge.operatingDay());
            Charge existing = rows.get(key);
            if (existing != null) {
                return new SavedCharge(existing, false);
            }
            Charge saved = charge.withId(ids.incrementAndGet());
            rows.put(key, saved);
            return new SavedCharge(saved, true);
        }

        synchronized int count() {
            return rows.size();
        }

        private static String key(String plate, long siteId, LocalDate day) {
            return plate + "|" + siteId + "|" + day;
        }
    }
}
