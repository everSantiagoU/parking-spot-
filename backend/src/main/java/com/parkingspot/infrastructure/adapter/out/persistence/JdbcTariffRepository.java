package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.TariffRepository;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTariffRepository implements TariffRepository {
    private final JdbcClient jdbc;

    public JdbcTariffRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Tariff> findFor(long siteId, VehicleType vehicleType, TimeSlot timeSlot) {
        return jdbc.sql("""
                        SELECT id, tipo_vehiculo, franja, valor
                        FROM tarifas
                        WHERE sede_id = :siteId AND tipo_vehiculo = :type AND franja = :slot
                        """)
                .param("siteId", siteId)
                .param("type", vehicleType.name())
                .param("slot", timeSlot.name())
                .query((row, index) -> new Tariff(
                        row.getLong("id"),
                        VehicleType.valueOf(row.getString("tipo_vehiculo")),
                        TimeSlot.valueOf(row.getString("franja")),
                        row.getBigDecimal("valor")))
                .optional();
    }
}
