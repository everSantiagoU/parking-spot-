package com.parkingspot.domain.model.history;

import com.parkingspot.domain.model.EventType;
import com.parkingspot.domain.model.VehicleType;
import java.time.OffsetDateTime;

/** Fila del historial de eventos de ingreso y salida (HU-20 CA-01). */
public record EventRecord(
        long id,
        long siteId,
        String plate,
        VehicleType vehicleType,
        EventType eventType,
        String source,
        String result,
        String reason,
        OffsetDateTime occurredAt) {
}
