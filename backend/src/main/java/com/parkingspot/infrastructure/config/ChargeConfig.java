package com.parkingspot.infrastructure.config;

import com.parkingspot.application.service.ChargeService;
import com.parkingspot.domain.port.out.ChargeRepository;
import com.parkingspot.domain.port.out.TariffRepository;
import com.parkingspot.domain.port.out.TimeSlotRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.PlateClassifier;
import com.parkingspot.domain.service.TimeSlotResolver;
import com.parkingspot.domain.service.tariff.CarTariffStrategy;
import com.parkingspot.domain.service.tariff.MotorcycleTariffStrategy;
import com.parkingspot.domain.service.tariff.TariffStrategyResolver;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma el motor de cobros (HU-14 y HU-15). La zona horaria define el día operativo. */
@Configuration
public class ChargeConfig {

    @Bean
    ZoneId parkingZone(@Value("${parking.time-zone:America/Bogota}") String zone) {
        return ZoneId.of(zone);
    }

    @Bean
    Clock parkingClock(ZoneId parkingZone) {
        return Clock.system(parkingZone);
    }

    @Bean
    OperatingDayCalculator operatingDayCalculator(ZoneId parkingZone) {
        return new OperatingDayCalculator(parkingZone);
    }

    @Bean
    TariffStrategyResolver tariffStrategyResolver() {
        return new TariffStrategyResolver(List.of(new CarTariffStrategy(), new MotorcycleTariffStrategy()));
    }

    @Bean
    ChargeService chargeService(OperatingDayCalculator operatingDays, TariffStrategyResolver strategies,
                                TimeSlotRepository timeSlots, TariffRepository tariffs, ChargeRepository charges,
                                Clock parkingClock) {
        return new ChargeService(new PlateClassifier(), operatingDays, new TimeSlotResolver(), strategies,
                timeSlots, tariffs, charges, parkingClock);
    }
}
