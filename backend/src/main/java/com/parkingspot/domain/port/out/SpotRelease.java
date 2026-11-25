package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.VehicleType;

/**
 * Operación de ocupación que necesita el carril de salida (HU-19).
 * Principio de segregación de interfaces (ISP): la salida solo libera cupos.
 */
public interface SpotRelease {
    /** Libera un cupo de forma atómica. Devuelve false si la fila no existe o no tiene ocupación válida. */
    boolean releaseOccupiedSpot(long siteId, VehicleType type);
}
