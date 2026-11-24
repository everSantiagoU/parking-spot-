package com.parkingspot.domain.service.tariff;

import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.VehicleType;
import java.math.BigDecimal;
import java.util.Optional;

/** RN-01: la moto tiene tarifa $0 por defecto y en ese caso no genera obligación de pago. */
public class MotorcycleTariffStrategy implements TariffStrategy {

    @Override
    public VehicleType vehicleType() {
        return VehicleType.MOTO;
    }

    @Override
    public Optional<BigDecimal> amountToCharge(Tariff tariff) {
        if (tariff.value().signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(tariff.value());
    }
}
