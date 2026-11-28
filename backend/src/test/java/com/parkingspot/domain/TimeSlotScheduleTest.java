package com.parkingspot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.TimeSlotSchedule;
import java.time.LocalTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * HU-12 CA-03 en el dominio: las franjas son configurables, así que la regla debe funcionar con horarios
 * distintos de 06:00/18:00. El inicio es inclusivo, el fin exclusivo y una franja puede cruzar la medianoche.
 */
class TimeSlotScheduleTest {
    private static final TimeSlotSchedule DAY = new TimeSlotSchedule(TimeSlot.DIURNA,
            LocalTime.of(7, 30), LocalTime.of(19, 30));
    private static final TimeSlotSchedule NIGHT = new TimeSlotSchedule(TimeSlot.NOCTURNA,
            LocalTime.of(19, 30), LocalTime.of(7, 30));

    @ParameterizedTest(name = "{0}: diurna={1}, nocturna={2}")
    @CsvSource({
            "07:29:59, false, true",   // un segundo antes del inicio diurno
            "07:30,    true,  false",  // inicio inclusivo
            "12:00,    true,  false",
            "19:29:59, true,  false",
            "19:30,    false, true",   // fin exclusivo de la diurna = inicio de la nocturna
            "23:59:59, false, true",
            "00:00,    false, true",   // cruza la medianoche
            "07:29,    false, true"
    })
    void cadaHoraPerteneceAExactamenteUnaFranja(String time, boolean inDay, boolean inNight) {
        var at = LocalTime.parse(time);
        assertThat(DAY.contains(at)).isEqualTo(inDay);
        assertThat(NIGHT.contains(at)).isEqualTo(inNight);
        assertThat(DAY.contains(at)).isNotEqualTo(NIGHT.contains(at));
    }
}
