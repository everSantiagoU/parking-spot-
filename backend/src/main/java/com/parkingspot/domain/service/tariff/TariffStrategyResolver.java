package com.parkingspot.domain.service.tariff;

import com.parkingspot.domain.model.VehicleType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Elige la estrategia de cobro según el tipo de vehículo. */
public class TariffStrategyResolver {
    private final Map<VehicleType, TariffStrategy> strategies = new EnumMap<>(VehicleType.class);

    public TariffStrategyResolver(List<TariffStrategy> available) {
        available.forEach(strategy -> strategies.put(strategy.vehicleType(), strategy));
    }

    public TariffStrategy strategyFor(VehicleType type) {
        TariffStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No hay una estrategia de cobro para " + type + ".");
        }
        return strategy;
    }
}
