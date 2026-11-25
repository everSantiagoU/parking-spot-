package com.parkingspot.infrastructure.config;

import com.parkingspot.application.service.PlateReadingService;
import com.parkingspot.application.usecase.ReadPlateUseCase;
import com.parkingspot.domain.port.out.PlateReaderPort;
import com.parkingspot.domain.service.PlateClassifier;
import com.parkingspot.infrastructure.adapter.out.plate.SimulatedPlateReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conecta el lector de placas. Para usar otro lector (por ejemplo, una cámara real),
 * basta con cambiar el bean plateReader: el dominio y la aplicación no cambian.
 */
@Configuration
public class PlateReaderConfig {
    @Bean
    PlateReaderPort plateReader() {
        return new SimulatedPlateReader();
    }

    @Bean
    ReadPlateUseCase readPlateUseCase(PlateReaderPort plateReader) {
        return new PlateReadingService(plateReader, new PlateClassifier());
    }
}
