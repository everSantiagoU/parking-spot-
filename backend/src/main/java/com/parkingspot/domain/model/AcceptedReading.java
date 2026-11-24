package com.parkingspot.domain.model;

import com.parkingspot.domain.exception.ReadingRequiresValidationException;

import java.util.Objects;

/**
 * Lectura que ya puede continuar al ingreso (CA-04 de la HU-11). Solo se crea de dos formas:
 * <ul>
 *     <li>{@link #automatic}: lectura del lector con confianza igual o mayor al umbral.</li>
 *     <li>{@link #manual}: placa digitada o confirmada por el operador, con origen MANUAL.</li>
 * </ul>
 * El registro de ingreso solo recibe este tipo, así que una lectura que requiere validación
 * no puede llegar a registrarse.
 */
public final class AcceptedReading {
    private final String plate;
    private final VehicleType type;
    private final ReadingSource source;
    private final Confidence confidence;
    private final Operator operator;

    private AcceptedReading(String plate, VehicleType type, ReadingSource source, Confidence confidence,
                            Operator operator) {
        this.plate = Objects.requireNonNull(plate, "plate");
        this.type = Objects.requireNonNull(type, "type");
        this.source = Objects.requireNonNull(source, "source");
        this.confidence = confidence;
        this.operator = Objects.requireNonNull(operator, "operator");
    }

    /** Acepta una lectura automática; falla si su confianza es menor que el umbral. */
    public static AcceptedReading automatic(PlateReading reading, ConfidenceThreshold threshold, Operator operator) {
        if (reading.source() == ReadingSource.MANUAL) {
            throw new IllegalArgumentException("Una lectura manual se acepta con AcceptedReading.manual.");
        }
        if (threshold.requiresValidation(reading.confidence())) {
            throw new ReadingRequiresValidationException();
        }
        return new AcceptedReading(reading.plate(), reading.type(), reading.source(), reading.confidence(), operator);
    }

    /** Placa digitada o confirmada por el operador: queda con origen MANUAL y su usuario, sin confianza. */
    public static AcceptedReading manual(VehicleClassification typedPlate, Operator operator) {
        return new AcceptedReading(typedPlate.plate(), typedPlate.type(), ReadingSource.MANUAL, null, operator);
    }

    public String plate() {
        return plate;
    }

    public VehicleType type() {
        return type;
    }

    public ReadingSource source() {
        return source;
    }

    /** Confianza del lector; es {@code null} en una lectura manual. */
    public Confidence confidence() {
        return confidence;
    }

    public Operator operator() {
        return operator;
    }
}
