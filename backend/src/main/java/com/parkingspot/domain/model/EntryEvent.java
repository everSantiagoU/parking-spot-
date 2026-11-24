package com.parkingspot.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Evento de ingreso para la trazabilidad del carril (HU-10).
 *
 * @param siteId     sede del operador autenticado
 * @param source     origen de la lectura (CA-04)
 * @param confidence confianza del lector; {@code null} en una lectura manual
 * @param reason     motivo del resultado; {@code null} si se autorizó
 * @param userId     operador que atendió el carril (obligatorio en una lectura manual)
 * @param occurredAt hora tomada del reloj del servidor (CA-02)
 */
public record EntryEvent(long siteId, String plate, VehicleType type, ReadingSource source, Confidence confidence,
                         EntryResult result, String reason, long userId, Instant occurredAt) {
    public EntryEvent {
        Objects.requireNonNull(plate, "plate");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (source == ReadingSource.MANUAL && confidence != null) {
            throw new IllegalArgumentException("Una lectura manual no tiene confianza del lector.");
        }
    }

    /** Ingreso autorizado de una lectura aceptada, en la sede y con el usuario del operador. */
    public static EntryEvent authorized(AcceptedReading reading, Instant occurredAt) {
        return new EntryEvent(reading.operator().siteId(), reading.plate(), reading.type(), reading.source(),
                reading.confidence(), EntryResult.AUTORIZADO, null, reading.operator().id(), occurredAt);
    }
}
