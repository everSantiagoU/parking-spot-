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

    /** Motivo que queda en eventos.motivo cuando no hay cupo (HU-13). */
    public static final String FULL_REASON = "LLENO";

    /** Ingreso autorizado de una lectura aceptada, en la sede y con el usuario del operador. */
    public static EntryEvent authorized(AcceptedReading reading, Instant occurredAt) {
        return of(reading, EntryResult.AUTORIZADO, null, occurredAt);
    }

    /** Ingreso rechazado porque no hay cupo para ese tipo de vehículo: queda la trazabilidad del intento. */
    public static EntryEvent rejectedFull(AcceptedReading reading, Instant occurredAt) {
        return of(reading, EntryResult.RECHAZADO, FULL_REASON, occurredAt);
    }

    private static EntryEvent of(AcceptedReading reading, EntryResult result, String reason, Instant occurredAt) {
        return new EntryEvent(reading.operator().siteId(), reading.plate(), reading.type(), reading.source(),
                reading.confidence(), result, reason, reading.operator().id(), occurredAt);
    }
}
