package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.TimeSlotSchedule;
import java.util.List;

/** Consulta de los horarios de franja de una sede. */
public interface TimeSlotRepository {
    List<TimeSlotSchedule> findBySite(long siteId);
}
