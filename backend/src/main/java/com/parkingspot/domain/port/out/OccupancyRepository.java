package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.VehicleType;

/** Estado de ocupación por sede y tipo de vehículo (tabla ocupacion de la HU-05). */
public interface OccupancyRepository {
    /**
     * Suma un vehículo a los ocupados.
     *
     * @throws com.parkingspot.domain.exception.OccupancyNotConfiguredException si la sede no tiene cupos para ese tipo
     */
    void occupySpot(long siteId, VehicleType type);
}
