package com.parkingspot.domain;

import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.service.PlateClassifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlateClassifierTest {
    private final PlateClassifier classifier = new PlateClassifier();

    @Test
    void classifiesAndNormalizesCarPlate() {
        var result = classifier.classify(" abc123 ");
        assertEquals("ABC123", result.plate());
        assertEquals(VehicleType.CARRO, result.type());
    }

    @Test
    void classifiesMotorcyclePlate() {
        assertEquals(VehicleType.MOTO, classifier.classify("abc12d").type());
    }

    @Test
    void rejectsInvalidPlates() {
        for (String plate : new String[] {"", "AB123", "ABC 123", "ABC12", "ABC12É", "ABC1234"}) {
            assertThrows(InvalidPlateException.class, () -> classifier.classify(plate));
        }
        assertThrows(InvalidPlateException.class, () -> classifier.classify(null));
    }
}
