package com.parkingspot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkingspot.domain.service.OperatingDayCalculator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** HU-15: el día operativo va de 06:00 a 05:59:59 del día siguiente (America/Bogota). */
class OperatingDayCalculatorTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private final OperatingDayCalculator calculator = new OperatingDayCalculator(BOGOTA);

    @ParameterizedTest(name = "{0} pertenece al día operativo {1}")
    @CsvSource({
            "2026-11-23T06:00:00, 2026-11-23",   // CA-01: inicio del día operativo
            "2026-11-23T05:59:59, 2026-11-22",   // CP-HU-15-E01: un segundo antes
            "2026-11-23T03:00:00, 2026-11-22",   // CA-02: madrugada
            "2026-11-23T00:00:00, 2026-11-22",
            "2026-11-23T23:59:59, 2026-11-23"
    })
    void calculaElDiaOperativo(String localDateTime, String expectedDay) {
        Instant instant = LocalDateTime.parse(localDateTime).atZone(BOGOTA).toInstant();

        assertThat(calculator.operatingDayOf(instant)).isEqualTo(LocalDate.parse(expectedDay));
    }

    @Test
    @DisplayName("Usa la hora de Bogotá aunque el instante venga en UTC")
    void usaLaZonaHorariaDeLaSede() {
        // 10:59 UTC son las 05:59 en Bogotá.
        assertThat(calculator.operatingDayOf(Instant.parse("2026-11-23T10:59:00Z")))
                .isEqualTo(LocalDate.of(2026, 11, 22));
    }

    @Test
    @DisplayName("El día operativo termina a las 06:00 del día siguiente")
    void finDelDiaOperativo() {
        assertThat(calculator.endOf(LocalDate.of(2026, 11, 22)).toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 11, 23, 6, 0));
    }
}
