package com.parkingspot.application.usecase;

import com.parkingspot.application.dto.ChargeResult;

/** HU-14 y HU-15: calcula y registra el cobro del día operativo al ingresar un vehículo. */
public interface GenerateChargeUseCase {
    ChargeResult generate(String plate, long siteId);
}
