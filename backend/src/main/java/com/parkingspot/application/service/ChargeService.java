package com.parkingspot.application.service;

import com.parkingspot.application.dto.ChargeResult;
import com.parkingspot.application.usecase.FindTodayChargeUseCase;
import com.parkingspot.application.usecase.GenerateChargeUseCase;
import com.parkingspot.domain.exception.ChargeNotFoundException;
import com.parkingspot.domain.exception.TariffNotConfiguredException;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.ChargeRepository;
import com.parkingspot.domain.port.out.ChargeRepository.SavedCharge;
import com.parkingspot.domain.port.out.TariffRepository;
import com.parkingspot.domain.port.out.TimeSlotRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.PlateClassifier;
import com.parkingspot.domain.service.TimeSlotResolver;
import com.parkingspot.domain.service.tariff.TariffStrategyResolver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Motor de cobros. Orquesta la clasificación de la placa, el día operativo, la franja,
 * la tarifa y la estrategia de cobro, y garantiza un solo cobro por placa, sede y día operativo.
 */
public class ChargeService implements GenerateChargeUseCase, FindTodayChargeUseCase {
    private final PlateClassifier plateClassifier;
    private final OperatingDayCalculator operatingDays;
    private final TimeSlotResolver timeSlotResolver;
    private final TariffStrategyResolver strategies;
    private final TimeSlotRepository timeSlots;
    private final TariffRepository tariffs;
    private final ChargeRepository charges;
    private final Clock clock;

    public ChargeService(PlateClassifier plateClassifier, OperatingDayCalculator operatingDays,
                         TimeSlotResolver timeSlotResolver, TariffStrategyResolver strategies,
                         TimeSlotRepository timeSlots, TariffRepository tariffs, ChargeRepository charges,
                         Clock clock) {
        this.plateClassifier = plateClassifier;
        this.operatingDays = operatingDays;
        this.timeSlotResolver = timeSlotResolver;
        this.strategies = strategies;
        this.timeSlots = timeSlots;
        this.tariffs = tariffs;
        this.charges = charges;
        this.clock = clock;
    }

    @Override
    public ChargeResult generate(String plate, long siteId) {
        VehicleClassification vehicle = plateClassifier.classify(plate);
        Instant now = clock.instant();
        LocalDate operatingDay = operatingDays.operatingDayOf(now);

        // HU-15 CA-03: si ya hay cobro en este día operativo, el reingreso no crea otro.
        Optional<Charge> existing = charges.findByPlateAndSiteAndDay(vehicle.plate(), siteId, operatingDay);
        if (existing.isPresent()) {
            return ChargeResult.existing(existing.get());
        }

        Tariff tariff = applicableTariff(siteId, vehicle.type(), now);

        Optional<BigDecimal> amount = strategies.strategyFor(vehicle.type()).amountToCharge(tariff);
        if (amount.isEmpty()) {
            return ChargeResult.exempt(vehicle, operatingDay, tariff.timeSlot());
        }

        Charge pending = Charge.pending(siteId, vehicle, operatingDay, tariff, amount.get(),
                operatingDays.localDateTimeOf(now));
        // HU-15 CA-06: si otra solicitud lo creó al mismo tiempo, se devuelve ese cobro.
        SavedCharge saved = charges.saveIfAbsent(pending);
        return saved.created() ? ChargeResult.created(saved.charge()) : ChargeResult.existing(saved.charge());
    }

    /** Evidencia que se captura al autorizar el ingreso, dentro de su transacción. */
    public Optional<LocalDate> exemptionAtEntry(long siteId, VehicleType type, Instant occurredAt) {
        if (type != VehicleType.MOTO) {
            return Optional.empty();
        }
        try {
            Tariff tariff = applicableTariff(siteId, type, occurredAt);
            if (strategies.strategyFor(type).amountToCharge(tariff).isEmpty()) {
                return Optional.of(operatingDays.operatingDayOf(occurredAt));
            }
        } catch (TariffNotConfiguredException exception) {
            // Sin configuración aplicable, el ingreso queda sin evidencia de exención.
        }
        return Optional.empty();
    }

    private Tariff applicableTariff(long siteId, VehicleType type, Instant now) {
        TimeSlot timeSlot = timeSlotResolver.resolve(timeSlots.findBySite(siteId), operatingDays.localTimeOf(now));
        return tariffs.findFor(siteId, type, timeSlot)
                .orElseThrow(() -> new TariffNotConfiguredException(
                        "No hay tarifa " + timeSlot + " configurada para " + type + "."));
    }

    @Override
    public Charge findToday(String plate, long siteId) {
        VehicleClassification vehicle = plateClassifier.classify(plate);
        LocalDate operatingDay = operatingDays.operatingDayOf(clock.instant());
        return charges.findByPlateAndSiteAndDay(vehicle.plate(), siteId, operatingDay)
                .orElseThrow(() -> new ChargeNotFoundException(vehicle.plate()));
    }
}
