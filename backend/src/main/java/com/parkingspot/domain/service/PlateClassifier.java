package com.parkingspot.domain.service;

import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.model.VehicleType;
import java.util.Locale;
import java.util.regex.Pattern;

public class PlateClassifier {
    private static final Pattern CAR_PLATE = Pattern.compile("[A-Z]{3}[0-9]{3}");
    private static final Pattern MOTORCYCLE_PLATE = Pattern.compile("[A-Z]{3}[0-9]{2}[A-Z]");

    public VehicleClassification classify(String plate) {
        if (plate == null) {
            throw new InvalidPlateException();
        }
        String normalized = plate.strip().toUpperCase(Locale.ROOT);
        if (CAR_PLATE.matcher(normalized).matches()) {
            return new VehicleClassification(normalized, VehicleType.CARRO);
        }
        if (MOTORCYCLE_PLATE.matcher(normalized).matches()) {
            return new VehicleClassification(normalized, VehicleType.MOTO);
        }
        throw new InvalidPlateException();
    }
}
