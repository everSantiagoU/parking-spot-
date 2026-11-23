package com.parkingspot.domain.service.tariff;

import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.VehicleType;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Patrón Strategy: cada tipo de vehículo tiene su propia regla de cobro.
 * Para agregar un tipo nuevo basta con crear otra estrategia (principio abierto/cerrado).
 */
public interface TariffStrategy {

    VehicleType vehicleType();

    /** Valor que se debe cobrar, o vacío si el vehículo no genera obligación de pago. */
    Optional<BigDecimal> amountToCharge(Tariff tariff);
}
