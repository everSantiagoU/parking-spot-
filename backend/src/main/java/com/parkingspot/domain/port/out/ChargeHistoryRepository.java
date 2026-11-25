package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.history.ChargeQuery;
import com.parkingspot.domain.model.history.ChargeRecord;
import com.parkingspot.domain.model.history.PageResult;

/** Consulta paginada del historial de cobros. */
public interface ChargeHistoryRepository {
    PageResult<ChargeRecord> find(ChargeQuery query);
}
