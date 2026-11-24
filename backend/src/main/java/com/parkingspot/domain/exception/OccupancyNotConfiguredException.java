package com.parkingspot.domain.exception;

import com.parkingspot.domain.model.VehicleType;

public class OccupancyNotConfiguredException extends RuntimeException {
    public OccupancyNotConfiguredException(long siteId, VehicleType type) {
        super("La sede " + siteId + " no tiene cupos configurados para " + type + ".");
    }
}
