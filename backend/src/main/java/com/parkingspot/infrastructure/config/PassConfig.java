package com.parkingspot.infrastructure.config;

import com.parkingspot.application.service.PassService;
import com.parkingspot.domain.port.out.PassRepository;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma la consulta de pases (HU-17). Usa el mismo reloj de la sede que cobros y pagos. */
@Configuration
public class PassConfig {

    @Bean
    PassService passService(PassRepository passes, Clock parkingClock) {
        return new PassService(new PlateClassifier(), passes, parkingClock);
    }
}
