package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.exception.OccupancyNotConfiguredException;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.OccupancyRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Actualiza la tabla ocupacion (HU-05) por sede y tipo de vehículo. */
@Repository
public class OccupancyStore implements OccupancyRepository {
    private final JdbcClient jdbc;

    public OccupancyStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void occupySpot(long siteId, VehicleType type) {
        int updated = jdbc.sql("""
                UPDATE ocupacion
                SET ocupados = ocupados + 1, actualizada_en = CURRENT_TIMESTAMP
                WHERE sede_id = :sede AND tipo_vehiculo = :tipo
                """)
                .param("sede", siteId)
                .param("tipo", type.name())
                .update();
        if (updated == 0) {
            throw new OccupancyNotConfiguredException(siteId, type);
        }
    }
}
