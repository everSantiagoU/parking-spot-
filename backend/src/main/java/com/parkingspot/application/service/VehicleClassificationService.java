package com.parkingspot.application.service;

import com.parkingspot.application.usecase.ClassifyVehicleUseCase;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.service.PlateClassifier;

public class VehicleClassificationService implements ClassifyVehicleUseCase {
    private final PlateClassifier classifier;

    public VehicleClassificationService(PlateClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public VehicleClassification classify(String plate) {
        return classifier.classify(plate);
    }
}
