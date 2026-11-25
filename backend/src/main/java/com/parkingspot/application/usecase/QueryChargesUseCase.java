package com.parkingspot.application.usecase;

import com.parkingspot.application.dto.HistoryFilter;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.history.ChargeRecord;
import com.parkingspot.domain.model.history.PageResult;

/** Consulta de cobros con filtros (HU-20 CA-02 a CA-05). */
public interface QueryChargesUseCase {
    PageResult<ChargeRecord> findCharges(HistoryFilter filter, ChargeStatus status, Long siteId);
}
