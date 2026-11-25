package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.model.history.ChargeQuery;
import com.parkingspot.domain.model.history.ChargeRecord;
import com.parkingspot.domain.model.history.PageResult;
import com.parkingspot.domain.port.out.ChargeHistoryRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Historial de cobros con SQL parametrizado: solo se agregan las condiciones de los filtros presentes. */
@Repository
public class JdbcChargeHistoryRepository implements ChargeHistoryRepository {
    private final JdbcClient jdbc;

    public JdbcChargeHistoryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult<ChargeRecord> find(ChargeQuery query) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (query.siteId() != null) {
            conditions.add("sede_id = :siteId");
            params.put("siteId", query.siteId());
        }
        if (query.plate() != null) {
            conditions.add("placa = :plate");
            params.put("plate", query.plate());
        }
        if (query.status() != null) {
            conditions.add("estado = :status");
            params.put("status", query.status().name());
        }
        if (query.from() != null) {
            conditions.add("dia_operativo >= :from");
            params.put("from", query.from());
        }
        if (query.to() != null) {
            conditions.add("dia_operativo <= :to");
            params.put("to", query.to());
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        long total = jdbc.sql("SELECT COUNT(*) FROM cobros" + where).params(params).query(Long.class).single();

        params.put("limit", query.size());
        params.put("offset", (long) query.page() * query.size());
        List<ChargeRecord> items = jdbc.sql("""
                        SELECT id, sede_id, placa, tipo_vehiculo, dia_operativo, franja, valor, estado,
                               generado_en, vigente_hasta
                        FROM cobros""" + where + " ORDER BY dia_operativo DESC, id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((row, index) -> new ChargeRecord(
                        row.getLong("id"),
                        row.getLong("sede_id"),
                        row.getString("placa"),
                        VehicleType.valueOf(row.getString("tipo_vehiculo")),
                        row.getObject("dia_operativo", LocalDate.class),
                        row.getString("franja"),
                        row.getBigDecimal("valor"),
                        ChargeStatus.valueOf(row.getString("estado")),
                        row.getObject("generado_en", OffsetDateTime.class),
                        row.getObject("vigente_hasta", OffsetDateTime.class)))
                .list();
        return new PageResult<>(items, query.page(), query.size(), total);
    }
}
