package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.exception.OccupancyNotConfiguredException;
import com.parkingspot.domain.model.Occupancy;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.OccupancyRepository;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Cupos y ocupación por sede y tipo de vehículo (tabla ocupacion de la HU-05; la capacidad la administra la HU-12).
 */
@Repository
public class OccupancyStore implements OccupancyRepository {
    private final JdbcClient jdbc;

    public OccupancyStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Verificar y ocupar son una sola sentencia: la base de datos bloquea la fila mientras la actualiza, y la
     * condición {@code ocupados < capacidad} se evalúa sobre el valor ya confirmado por otras transacciones.
     * Si dos ingresos compiten por el último cupo, solo uno afecta la fila; el otro afecta 0 filas → LLENO.
     */
    @Override
    public boolean occupySpotIfAvailable(long siteId, VehicleType type) {
        int updated = jdbc.sql("""
                UPDATE ocupacion
                SET ocupados = ocupados + 1, actualizada_en = CURRENT_TIMESTAMP
                WHERE sede_id = :sede AND tipo_vehiculo = :tipo AND ocupados < capacidad
                """)
                .param("sede", siteId)
                .param("tipo", type.name())
                .update();
        if (updated == 1) {
            return true;
        }
        // 0 filas: o no hay cupo, o la sede no tiene cupos configurados para ese tipo.
        boolean configured = jdbc.sql("SELECT COUNT(*) FROM ocupacion WHERE sede_id = :sede AND tipo_vehiculo = :tipo")
                .param("sede", siteId)
                .param("tipo", type.name())
                .query(Integer.class)
                .single() > 0;
        if (!configured) {
            throw new OccupancyNotConfiguredException(siteId, type);
        }
        return false;
    }

    @Override
    public boolean releaseOccupiedSpot(long siteId, VehicleType type) {
        return jdbc.sql("""
                UPDATE ocupacion
                SET ocupados = ocupados - 1, actualizada_en = CURRENT_TIMESTAMP
                WHERE sede_id = :sede AND tipo_vehiculo = :tipo
                  AND ocupados > 0 AND ocupados <= capacidad
                """)
                .param("sede", siteId)
                .param("tipo", type.name())
                .update() == 1;
    }

    @Override
    public List<Occupancy> findBySite(long siteId) {
        return jdbc.sql("""
                SELECT tipo_vehiculo, capacidad, ocupados
                FROM ocupacion WHERE sede_id = :sede
                ORDER BY tipo_vehiculo
                """)
                .param("sede", siteId)
                .query((row, index) -> new Occupancy(VehicleType.valueOf(row.getString("tipo_vehiculo")),
                        row.getInt("capacidad"), row.getInt("ocupados")))
                .list();
    }
}
