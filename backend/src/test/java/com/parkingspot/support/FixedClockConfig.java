package com.parkingspot.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Reloj fijo para comprobar que la hora del evento sale del servidor y no de la petición. */
@TestConfiguration
public class FixedClockConfig {
    public static final Instant NOW = Instant.parse("2026-11-24T18:45:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
