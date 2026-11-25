package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Occupancy;
import com.parkingspot.domain.model.VehicleType;
import java.util.List;

/** Estado de ocupación por sede y tipo de vehículo (tabla ocupacion de la HU-05). */
public interface OccupancyRepository {
    /**
     * Ocupa un cupo solo si hay disponibilidad, de forma atómica: la verificación y el incremento son una
     * sola operación, así que dos ingresos simultáneos no pueden tomar el último cupo a la vez (HU-13).
     *
     * @return {@code true} si se ocupó el cupo; {@code false} si la sede está LLENA para ese tipo
     * @throws com.parkingspot.domain.exception.OccupancyNotConfiguredException si la sede no tiene cupos para ese tipo
     */
    boolean occupySpotIfAvailable(long siteId, VehicleType type);

    /** Libera un cupo de forma atómica. Devuelve false si la fila no existe o no tiene ocupación válida. */
    boolean releaseOccupiedSpot(long siteId, VehicleType type);

    List<Occupancy> findBySite(long siteId);
}
