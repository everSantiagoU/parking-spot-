package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Occupancy;
import java.util.List;

/**
 * Consulta de ocupación para mostrarla en pantalla.
 * Principio de segregación de interfaces (ISP): quien solo consulta no puede ocupar ni liberar cupos.
 */
public interface OccupancyQuery {
    List<Occupancy> findBySite(long siteId);
}
