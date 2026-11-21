package com.parkingspot.domain;

import com.parkingspot.domain.exception.InvalidConfidenceException;
import com.parkingspot.domain.model.Confidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidenceTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.2, 0.5, 1.0})
    void acceptsValuesBetweenZeroAndOneInclusive(double value) {
        assertEquals(value, Confidence.of(value).value());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 1.01, -1.0, 2.0, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsValuesOutsideRange(double value) {
        assertThrows(InvalidConfidenceException.class, () -> new Confidence(value));
    }

    @Test
    void rejectsMissingValue() {
        assertThrows(InvalidConfidenceException.class, () -> Confidence.of(null));
    }
}
