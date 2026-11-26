package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.VehicleType;

/**
 * Operaciones de ocupación que necesita el carril de ingreso (HU-13).
 * Principio de segregación de interfaces (ISP): el ingreso no ve las operaciones de salida ni de consulta.
 */
public interface SpotReservation {
    /** Serializa la consulta de ingreso activo y la reserva hasta el fin de la transacción. */
    void lockForEntry(long siteId, VehicleType type);

    /**
     * Ocupa un cupo solo si hay disponibilidad, de forma atómica: la verificación y el incremento son una
     * sola operación, así que dos ingresos simultáneos no pueden tomar el último cupo a la vez (HU-13).
     *
     * @return {@code true} si se ocupó el cupo; {@code false} si la sede está LLENA para ese tipo
     * @throws com.parkingspot.domain.exception.OccupancyNotConfiguredException si la sede no tiene cupos para ese tipo
     */
    boolean occupySpotIfAvailable(long siteId, VehicleType type);
}
