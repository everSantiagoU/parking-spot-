package com.parkingspot.infrastructure.config;

import com.parkingspot.application.service.VehicleClassificationService;
import com.parkingspot.application.usecase.ClassifyVehicleUseCase;
import com.parkingspot.domain.service.PlateClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VehicleClassificationConfig {
    @Bean
    ClassifyVehicleUseCase classifyVehicleUseCase() {
        return new VehicleClassificationService(new PlateClassifier());
    }
}
