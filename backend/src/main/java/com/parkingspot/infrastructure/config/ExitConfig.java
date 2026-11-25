package com.parkingspot.infrastructure.config;

import com.parkingspot.application.service.ExitValidationService;
import com.parkingspot.application.usecase.ValidateExitUseCase;
import com.parkingspot.domain.port.out.ChargeRepository;
import com.parkingspot.domain.port.out.ExitBarrier;
import com.parkingspot.domain.port.out.ExitEventRepository;
import com.parkingspot.domain.port.out.OperatorRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExitConfig {
    @Bean
    ValidateExitUseCase validateExitUseCase(OperatorRepository operators, ExitEventRepository events,
                                           ChargeRepository charges, OperatingDayCalculator operatingDays,
                                           ExitBarrier barrier, Clock parkingClock) {
        return new ExitValidationService(new PlateClassifier(), operators, events, charges, operatingDays, barrier,
                parkingClock);
    }
}
