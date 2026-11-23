package com.parkingspot.domain.service.tariff;

import com.parkingspot.domain.exception.TariffNotConfiguredException;
import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.VehicleType;
import java.math.BigDecimal;
import java.util.Optional;

/** Un carro siempre genera cobro: una tarifa de $0 para carros es una configuración inválida. */
public class CarTariffStrategy implements TariffStrategy {

    @Override
    public VehicleType vehicleType() {
        return VehicleType.CARRO;
    }

    @Override
    public Optional<BigDecimal> amountToCharge(Tariff tariff) {
        if (tariff.value().signum() <= 0) {
            throw new TariffNotConfiguredException(
                    "La tarifa " + tariff.timeSlot() + " de carro debe ser mayor que $0.");
        }
        return Optional.of(tariff.value());
    }
}
