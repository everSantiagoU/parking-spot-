package com.parkingspot.infrastructure.config;

import com.parkingspot.application.service.HistoryService;
import com.parkingspot.domain.port.out.ChargeHistoryRepository;
import com.parkingspot.domain.port.out.EventHistoryRepository;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma la consulta del historial (HU-20). Las fechas del filtro se leen en la zona de la sede. */
@Configuration
public class HistoryConfig {

    @Bean
    HistoryService historyService(EventHistoryRepository events, ChargeHistoryRepository charges, ZoneId parkingZone) {
        return new HistoryService(new PlateClassifier(), events, charges, parkingZone);
    }
}
