package com.parkingspot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.domain.exception.TariffNotConfiguredException;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.TimeSlotSchedule;
import com.parkingspot.domain.service.TimeSlotResolver;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeSlotResolverTest {
    private static final List<TimeSlotSchedule> SCHEDULES = List.of(
            new TimeSlotSchedule(TimeSlot.DIURNA, LocalTime.of(6, 0), LocalTime.of(18, 0)),
            new TimeSlotSchedule(TimeSlot.NOCTURNA, LocalTime.of(18, 0), LocalTime.of(6, 0)));

    private final TimeSlotResolver resolver = new TimeSlotResolver();

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"06:00, DIURNA", "10:00, DIURNA", "17:59, DIURNA", "18:00, NOCTURNA", "23:30, NOCTURNA",
            "03:00, NOCTURNA", "05:59, NOCTURNA"})
    void resuelveLaFranja(String time, TimeSlot expected) {
        assertThat(resolver.resolve(SCHEDULES, LocalTime.parse(time))).isEqualTo(expected);
    }

    @Test
    void sinFranjaConfiguradaEsUnErrorControlado() {
        List<TimeSlotSchedule> soloDiurna = List.of(SCHEDULES.get(0));

        assertThatThrownBy(() -> resolver.resolve(soloDiurna, LocalTime.of(20, 0)))
                .isInstanceOf(TariffNotConfiguredException.class);
    }
}
