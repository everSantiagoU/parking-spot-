package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleType;
import java.util.Optional;

/** Consulta de tarifas configuradas. */
public interface TariffRepository {
    Optional<Tariff> findFor(long siteId, VehicleType vehicleType, TimeSlot timeSlot);
}
