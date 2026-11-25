package com.parkingspot.domain.model;

import java.util.Objects;

/**
 * Lo que entrega cualquier lector: el texto leído (aún sin normalizar ni clasificar),
 * una confianza válida y la fuente que produjo la lectura.
 */
public record RawPlateReading(String plateText, Confidence confidence, ReadingSource source) {
    public RawPlateReading {
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(source, "source");
    }
}
