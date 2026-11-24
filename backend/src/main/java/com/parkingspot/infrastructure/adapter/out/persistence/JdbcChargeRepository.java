package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.ChargeRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcChargeRepository implements ChargeRepository {
    static final String COLUMNS = """
            id, sede_id, placa, tipo_vehiculo, dia_operativo, franja, tarifa_id,
            tarifa_aplicada, valor, estado, generado_en
            """;

    private final JdbcClient jdbc;

    public JdbcChargeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Charge> findByPlateAndSiteAndDay(String plate, long siteId, LocalDate operatingDay) {
        return jdbc.sql("SELECT " + COLUMNS + """
                        FROM cobros
                        WHERE placa = :plate AND sede_id = :siteId AND dia_operativo = :day
                        """)
                .param("plate", plate)
                .param("siteId", siteId)
                .param("day", operatingDay)
                .query((row, index) -> toCharge(row))
                .optional();
    }

    /**
     * La restricción uq_cobros_placa_sede_dia (HU-05) decide: si otra transacción ya insertó
     * el cobro, ON CONFLICT DO NOTHING no inserta nada y se devuelve el cobro existente.
     */
    @Override
    public SavedCharge saveIfAbsent(Charge charge) {
        Optional<Long> newId = jdbc.sql("""
                        INSERT INTO cobros (sede_id, placa, tipo_vehiculo, dia_operativo, franja, tarifa_id,
                                            tarifa_aplicada, valor, estado, generado_en)
                        VALUES (:siteId, :plate, :type, :day, :slot, :tariffId,
                                :appliedTariff, :amount, :status, :generatedAt)
                        ON CONFLICT ON CONSTRAINT uq_cobros_placa_sede_dia DO NOTHING
                        RETURNING id
                        """)
                .param("siteId", charge.siteId())
                .param("plate", charge.plate())
                .param("type", charge.vehicleType().name())
                .param("day", charge.operatingDay())
                .param("slot", charge.timeSlot().name())
                .param("tariffId", charge.tariffId())
                .param("appliedTariff", charge.appliedTariff())
                .param("amount", charge.amount())
                .param("status", charge.status().name())
                .param("generatedAt", charge.generatedAt())
                .query(Long.class)
                .optional();

        if (newId.isPresent()) {
            return new SavedCharge(charge.withId(newId.get()), true);
        }
        Charge existing = findByPlateAndSiteAndDay(charge.plate(), charge.siteId(), charge.operatingDay())
                .orElseThrow(() -> new IllegalStateException("El cobro en conflicto no se encontró."));
        return new SavedCharge(existing, false);
    }

    static Charge toCharge(ResultSet row) throws SQLException {
        return new Charge(
                row.getLong("id"),
                row.getLong("sede_id"),
                row.getString("placa"),
                VehicleType.valueOf(row.getString("tipo_vehiculo")),
                row.getObject("dia_operativo", LocalDate.class),
                TimeSlot.valueOf(row.getString("franja")),
                row.getLong("tarifa_id"),
                row.getBigDecimal("tarifa_aplicada"),
                row.getBigDecimal("valor"),
                ChargeStatus.valueOf(row.getString("estado")),
                row.getObject("generado_en", OffsetDateTime.class));
    }
}
