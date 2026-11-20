package com.parkingspot.application.usecase;

import com.parkingspot.domain.model.VehicleClassification;

public interface ClassifyVehicleUseCase {
    VehicleClassification classify(String plate);
}
