package com.parkingspot.infrastructure.config;

import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.service.ChargeService;
import com.parkingspot.application.service.EntryRegistrationService;
import com.parkingspot.application.usecase.RegisterEntryUseCase;
import com.parkingspot.application.usecase.ValidateEntryReadingUseCase;
import com.parkingspot.domain.port.out.EntryEventRepository;
import com.parkingspot.domain.port.out.ExitEventRepository;
import com.parkingspot.domain.port.out.OccupancyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Conecta el registro de ingresos de la HU-10. */
@Configuration
public class EntryRegistrationConfig {
    /**
     * Reloj del servidor para la hora del evento. Las pruebas lo reemplazan por un Clock fijo.
     * Se llama entryClock para no chocar con otros relojes del proyecto (por ejemplo, parkingClock).
     */
    @Bean
    Clock entryClock() {
        return Clock.systemUTC();
    }

    @Bean
    RegisterEntryUseCase registerEntryUseCase(ValidateEntryReadingUseCase validateEntryReadingUseCase,
                                              EntryEventRepository events, OccupancyRepository occupancy,
                                              Transactions transactions, Clock entryClock, ChargeService charges,
                                              ExitEventRepository exits) {
        return new EntryRegistrationService(validateEntryReadingUseCase, events, occupancy, transactions, entryClock,
                charges, exits);
    }
}
