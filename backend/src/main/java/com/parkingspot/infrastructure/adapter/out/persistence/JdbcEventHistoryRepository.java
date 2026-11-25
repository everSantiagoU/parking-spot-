package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.EventType;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.model.history.EventQuery;
import com.parkingspot.domain.model.history.EventRecord;
import com.parkingspot.domain.model.history.PageResult;
import com.parkingspot.domain.port.out.EventHistoryRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Historial de eventos con SQL parametrizado: solo se agregan las condiciones de los filtros presentes. */
@Repository
public class JdbcEventHistoryRepository implements EventHistoryRepository {
    private final JdbcClient jdbc;

    public JdbcEventHistoryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult<EventRecord> find(EventQuery query) {
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
        if (query.type() != null) {
            conditions.add("tipo_evento = :type");
            params.put("type", query.type().name());
        }
        if (query.from() != null) {
            conditions.add("ocurrido_en >= :from");
            params.put("from", OffsetDateTime.ofInstant(query.from(), ZoneOffset.UTC));
        }
        if (query.to() != null) {
            conditions.add("ocurrido_en < :to");
            params.put("to", OffsetDateTime.ofInstant(query.to(), ZoneOffset.UTC));
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        long total = jdbc.sql("SELECT COUNT(*) FROM eventos" + where).params(params).query(Long.class).single();

        params.put("limit", query.size());
        params.put("offset", (long) query.page() * query.size());
        List<EventRecord> items = jdbc.sql("""
                        SELECT id, sede_id, placa, tipo_vehiculo, tipo_evento, origen_lectura, resultado, motivo, ocurrido_en
                        FROM eventos""" + where + " ORDER BY ocurrido_en DESC, id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((row, index) -> new EventRecord(
                        row.getLong("id"),
                        row.getLong("sede_id"),
                        row.getString("placa"),
                        VehicleType.valueOf(row.getString("tipo_vehiculo")),
                        EventType.valueOf(row.getString("tipo_evento")),
                        row.getString("origen_lectura"),
                        row.getString("resultado"),
                        row.getString("motivo"),
                        row.getObject("ocurrido_en", OffsetDateTime.class)))
                .list();
        return new PageResult<>(items, query.page(), query.size(), total);
    }
}
