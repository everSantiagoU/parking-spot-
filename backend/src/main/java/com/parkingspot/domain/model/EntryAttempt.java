package com.parkingspot.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Resultado de un intento de ingreso en el carril.
 *
 * @param reason     motivo cuando no se autoriza (por ejemplo, CONFIANZA_BAJA); {@code null} si se autorizó
 * @param eventId    evento guardado; {@code null} si no se registró ningún evento
 * @param occurredAt hora del servidor del evento; {@code null} si no se registró
 */
public record EntryAttempt(EntryOutcome outcome, String reason, String readText, String plate, VehicleType type,
                           Confidence confidence, ReadingSource source, String operatorUsername, Long siteId,
                           Long eventId, Instant occurredAt) {
    public EntryAttempt {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(operatorUsername, "operatorUsername");
    }

    /** La barrera solo se abre cuando la entrada se autoriza. */
    public boolean barrierOpen() {
        return outcome == EntryOutcome.AUTORIZADO;
    }

    /** Sin evento ni sede: una lectura que requiere validación no registra nada (CA-04 de la HU-11). */
    public static EntryAttempt requiresValidation(ReadingEvaluation evaluation, String operatorUsername) {
        return new EntryAttempt(EntryOutcome.REQUIERE_VALIDACION, evaluation.reason().name(), evaluation.readText(),
                evaluation.plate(), evaluation.type(), evaluation.confidence(), evaluation.source(),
                operatorUsername, null, null, null);
    }

    public static EntryAttempt registered(EntryOutcome outcome, String readText, EntryEvent event, long eventId,
                                          String operatorUsername) {
        return new EntryAttempt(outcome, event.reason(), readText, event.plate(), event.type(), event.confidence(),
                event.source(), operatorUsername, event.siteId(), eventId, event.occurredAt());
    }
}
