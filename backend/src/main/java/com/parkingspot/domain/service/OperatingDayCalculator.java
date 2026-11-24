package com.parkingspot.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Calcula el día operativo (S-02): va de las 06:00 hasta las 05:59:59 del día siguiente,
 * en la zona horaria de la sede. Un evento a las 03:00 del 15 pertenece al día operativo del 14.
 */
public class OperatingDayCalculator {
    public static final LocalTime DAY_START = LocalTime.of(6, 0);

    private final ZoneId zone;

    public OperatingDayCalculator(ZoneId zone) {
        this.zone = zone;
    }

    public LocalDate operatingDayOf(Instant instant) {
        ZonedDateTime local = instant.atZone(zone);
        LocalDate calendarDay = local.toLocalDate();
        return local.toLocalTime().isBefore(DAY_START) ? calendarDay.minusDays(1) : calendarDay;
    }

    public LocalTime localTimeOf(Instant instant) {
        return instant.atZone(zone).toLocalTime();
    }

    public OffsetDateTime localDateTimeOf(Instant instant) {
        return instant.atZone(zone).toOffsetDateTime();
    }

    /** Primer instante que ya no pertenece al día operativo (06:00 del día siguiente). */
    public OffsetDateTime endOf(LocalDate operatingDay) {
        return operatingDay.plusDays(1).atTime(DAY_START).atZone(zone).toOffsetDateTime();
    }
}
