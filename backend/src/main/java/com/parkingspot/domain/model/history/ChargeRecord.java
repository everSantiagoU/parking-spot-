package com.parkingspot.domain.model.history;

import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.VehicleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Fila del historial de cobros: importe, fecha, placa y estado (HU-20 CA-02). */
public record ChargeRecord(
        long id,
        long siteId,
        String plate,
        VehicleType vehicleType,
        LocalDate operatingDay,
        String timeSlot,
        BigDecimal amount,
        ChargeStatus status,
        OffsetDateTime generatedAt,
        OffsetDateTime validUntil) {
}
