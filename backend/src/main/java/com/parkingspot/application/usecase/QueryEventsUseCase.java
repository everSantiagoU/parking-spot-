package com.parkingspot.application.usecase;

import com.parkingspot.application.dto.HistoryFilter;
import com.parkingspot.domain.model.EventType;
import com.parkingspot.domain.model.history.EventRecord;
import com.parkingspot.domain.model.history.PageResult;

/** Consulta de ingresos y salidas con filtros (HU-20 CA-01, CA-03, CA-04). */
public interface QueryEventsUseCase {
    PageResult<EventRecord> findEvents(HistoryFilter filter, EventType type, Long siteId);
}
