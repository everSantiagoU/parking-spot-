package com.parkingspot.infrastructure.config;

import com.parkingspot.application.service.EntryReadingValidationService;
import com.parkingspot.application.usecase.ValidateEntryReadingUseCase;
import com.parkingspot.domain.model.ConfidenceThreshold;
import com.parkingspot.domain.port.out.OperatorRepository;
import com.parkingspot.domain.port.out.PlateReaderPort;
import com.parkingspot.domain.service.PlateClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Conecta la validación de lecturas de la HU-11. */
@Configuration
public class EntryReadingConfig {
    /**
     * Umbral configurable con parking.entry.confidence-threshold (variable PLATE_CONFIDENCE_THRESHOLD).
     * Si está fuera de [0, 1], la aplicación no arranca.
     */
    @Bean
    ConfidenceThreshold confidenceThreshold(@Value("${parking.entry.confidence-threshold:0.80}") double value) {
        return new ConfidenceThreshold(value);
    }

    @Bean
    ValidateEntryReadingUseCase validateEntryReadingUseCase(PlateReaderPort plateReader,
                                                            ConfidenceThreshold confidenceThreshold,
                                                            OperatorRepository operators) {
        return new EntryReadingValidationService(plateReader, new PlateClassifier(), confidenceThreshold, operators);
    }
}
