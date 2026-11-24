package com.parkingspot.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Resultado de evaluar una lectura automática contra el umbral y el formato de placa.
 * Si requiere validación, no trae una {@link AcceptedReading}: no hay nada que registrar.
 *
 * @param readText   texto tal como lo entregó el lector
 * @param plate      placa normalizada, o {@code null} si el formato es inválido
 * @param type       tipo de vehículo, o {@code null} si el formato es inválido
 * @param reason     motivo de la validación, o {@code null} si se aceptó
 * @param accepted   lectura aceptada, o {@code null} si requiere validación
 */
public record ReadingEvaluation(ValidationStatus status, ValidationReason reason, String readText, String plate,
                                VehicleType type, Confidence confidence, ReadingSource source,
                                AcceptedReading accepted) {
    public ReadingEvaluation {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(source, "source");
        if ((status == ValidationStatus.ACEPTADA) != (accepted != null)) {
            throw new IllegalArgumentException("Solo una lectura aceptada trae AcceptedReading.");
        }
        if ((status == ValidationStatus.REQUIERE_VALIDACION) != (reason != null)) {
            throw new IllegalArgumentException("Solo una lectura que requiere validación trae motivo.");
        }
    }

    public static ReadingEvaluation accepted(String readText, AcceptedReading accepted) {
        return new ReadingEvaluation(ValidationStatus.ACEPTADA, null, readText, accepted.plate(), accepted.type(),
                accepted.confidence(), accepted.source(), accepted);
    }

    public static ReadingEvaluation requiresValidation(ValidationReason reason, String readText, String plate,
                                                       VehicleType type, Confidence confidence,
                                                       ReadingSource source) {
        return new ReadingEvaluation(ValidationStatus.REQUIERE_VALIDACION, reason, readText, plate, type,
                confidence, source, null);
    }

    public Optional<AcceptedReading> acceptedReading() {
        return Optional.ofNullable(accepted);
    }
}
