package com.parkingspot.domain.model;

import java.time.LocalTime;

/**
 * Horario de una franja. La hora de inicio es inclusiva y la de fin exclusiva.
 * Si el inicio es posterior al fin, la franja cruza la medianoche (por ejemplo, 18:00 a 06:00).
 */
public record TimeSlotSchedule(TimeSlot timeSlot, LocalTime start, LocalTime end) {

    public boolean contains(LocalTime time) {
        boolean crossesMidnight = start.isAfter(end);
        if (crossesMidnight) {
            return !time.isBefore(start) || time.isBefore(end);
        }
        return !time.isBefore(start) && time.isBefore(end);
    }
}
