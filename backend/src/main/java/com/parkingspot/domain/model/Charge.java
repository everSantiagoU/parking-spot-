package com.parkingspot.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Cobro del día operativo de una placa en una sede.
 * Guarda una copia de la tarifa con la que se calculó (RN-07), así un cambio
 * posterior de la tarifa no altera los cobros ya generados.
 */
public record Charge(
        Long id,
        long siteId,
        String plate,
        VehicleType vehicleType,
        LocalDate operatingDay,
        TimeSlot timeSlot,
        long tariffId,
        BigDecimal appliedTariff,
        BigDecimal amount,
        ChargeStatus status,
        OffsetDateTime generatedAt) {

    /** Crea un cobro nuevo, todavía sin identificador, en estado PENDIENTE. */
    public static Charge pending(long siteId, VehicleClassification vehicle, LocalDate operatingDay,
                                 Tariff tariff, BigDecimal amount, OffsetDateTime generatedAt) {
        return new Charge(null, siteId, vehicle.plate(), vehicle.type(), operatingDay, tariff.timeSlot(),
                tariff.id(), tariff.value(), amount, ChargeStatus.PENDIENTE, generatedAt);
    }

    public Charge withId(long newId) {
        return new Charge(newId, siteId, plate, vehicleType, operatingDay, timeSlot, tariffId, appliedTariff,
                amount, status, generatedAt);
    }
}
