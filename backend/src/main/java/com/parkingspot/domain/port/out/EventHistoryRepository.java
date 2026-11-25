package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.history.EventQuery;
import com.parkingspot.domain.model.history.EventRecord;
import com.parkingspot.domain.model.history.PageResult;

/** Consulta paginada del historial de eventos. */
public interface EventHistoryRepository {
    PageResult<EventRecord> find(EventQuery query);
}
