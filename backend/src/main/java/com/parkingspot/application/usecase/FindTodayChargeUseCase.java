package com.parkingspot.application.usecase;

import com.parkingspot.domain.model.Charge;

/** Consulta el cobro de una placa en el día operativo actual. */
public interface FindTodayChargeUseCase {
    Charge findToday(String plate, long siteId);
}
