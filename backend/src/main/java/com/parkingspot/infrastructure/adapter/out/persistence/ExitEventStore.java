package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.EntryResult;
import com.parkingspot.domain.model.ExitDecision;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.ExitEventRepository;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ExitEventStore implements ExitEventRepository {
    private final JdbcClient jdbc;

    public ExitEventStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ActiveEntry> findActiveEntry(long siteId, String plate) {
        // Las salidas históricas sin vínculo se evalúan por orden de movimiento; las nuevas, por ingreso_id.
        return jdbc.sql("""
                SELECT movimiento.id, movimiento.tipo_vehiculo, movimiento.dia_operativo_exento
                FROM (
                    SELECT id, tipo_vehiculo, dia_operativo_exento, tipo_evento
                    FROM eventos
                    WHERE sede_id = :site AND placa = :plate AND resultado = 'AUTORIZADO'
                      AND tipo_evento IN ('INGRESO', 'SALIDA') AND ingreso_id IS NULL
                    ORDER BY ocurrido_en DESC, id DESC
                    LIMIT 1
                ) AS movimiento
                WHERE movimiento.tipo_evento = 'INGRESO'
                  AND NOT EXISTS (SELECT 1 FROM eventos salida WHERE salida.ingreso_id = movimiento.id)
                """)
                .param("site", siteId)
                .param("plate", plate)
                .query((row, index) -> new ActiveEntry(row.getLong("id"),
                        VehicleType.valueOf(row.getString("tipo_vehiculo")),
                        row.getObject("dia_operativo_exento", LocalDate.class)))
                .optional();
    }

    @Override
    public boolean recordEffectiveExit(Operator operator, ActiveEntry entry, Instant now, ExitDecision decision,
                                       Long chargeId) {
        return jdbc.sql("""
                INSERT INTO eventos (sede_id, placa, tipo_vehiculo, tipo_evento, origen_lectura,
                                     resultado, motivo, usuario_id, ocurrido_en, cobro_id, ingreso_id)
                SELECT sede_id, placa, tipo_vehiculo, 'SALIDA', 'MANUAL',
                       'AUTORIZADO', :reason, :operator, :occurredAt, :charge, id
                FROM eventos
                WHERE id = :entry AND sede_id = :site AND tipo_evento = 'INGRESO' AND resultado = 'AUTORIZADO'
                ON CONFLICT ON CONSTRAINT uq_eventos_salida_por_ingreso DO NOTHING
                """)
                .param("entry", entry.id())
                .param("site", operator.siteId())
                .param("reason", decision.name())
                .param("operator", operator.id())
                .param("occurredAt", OffsetDateTime.ofInstant(now, ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("charge", chargeId, Types.BIGINT)
                .update() == 1;
    }

    @Override
    public void recordAttempt(Operator operator, VehicleClassification vehicle, Instant now, ExitDecision decision,
                              Long chargeId) {
        int written = jdbc.sql("""
                INSERT INTO eventos (sede_id, placa, tipo_vehiculo, tipo_evento, origen_lectura,
                                     resultado, motivo, usuario_id, ocurrido_en, cobro_id)
                VALUES (:site, :plate, :type, 'VALIDACION', 'MANUAL',
                        :result, :reason, :operator, :occurredAt, :charge)
                """)
                .param("site", operator.siteId())
                .param("plate", vehicle.plate())
                .param("type", vehicle.type().name())
                .param("result", (decision.authorizesOpening() ? EntryResult.AUTORIZADO : EntryResult.RECHAZADO).name())
                .param("reason", decision.name())
                .param("operator", operator.id())
                .param("occurredAt", OffsetDateTime.ofInstant(now, ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("charge", chargeId, Types.BIGINT)
                .update();
        if (written != 1) {
            throw new IllegalStateException("No se registró el intento de salida.");
        }
    }
}
