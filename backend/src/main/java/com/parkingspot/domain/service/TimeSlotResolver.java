package com.parkingspot.domain.service;

import com.parkingspot.domain.exception.TariffNotConfiguredException;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.TimeSlotSchedule;
import java.time.LocalTime;
import java.util.List;

/** Encuentra la franja (diurna o nocturna) que corresponde a una hora local. */
public class TimeSlotResolver {

    public TimeSlot resolve(List<TimeSlotSchedule> schedules, LocalTime time) {
        return schedules.stream()
                .filter(schedule -> schedule.contains(time))
                .map(TimeSlotSchedule::timeSlot)
                .findFirst()
                .orElseThrow(() -> new TariffNotConfiguredException(
                        "No hay una franja horaria configurada para las " + time.withNano(0) + "."));
    }
}
