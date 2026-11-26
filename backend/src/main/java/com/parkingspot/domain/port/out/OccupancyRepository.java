package com.parkingspot.domain.port.out;

/**
 * Estado de ocupación por sede y tipo de vehículo (tabla ocupacion de la HU-05).
 * Reúne las tres interfaces pequeñas para que un solo adaptador ({@code OccupancyStore}) las implemente;
 * cada cliente depende solo de la parte que usa (ISP).
 */
public interface OccupancyRepository extends SpotReservation, SpotRelease, OccupancyQuery {
}
