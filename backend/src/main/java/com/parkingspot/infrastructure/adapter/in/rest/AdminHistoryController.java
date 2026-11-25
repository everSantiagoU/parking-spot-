package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.service.PlateClassifier;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/history")
public record AdminHistoryController(JdbcClient jdbc, ZoneId parkingZone) {
    public enum Kind { EVENTOS, COBROS }
    public record Row(long id, String site, String plate, OffsetDateTime date, String kind, String status,
                      String reason, BigDecimal amount) {}

    @GetMapping
    @Operation(summary = "Consulta eventos o cobros por placa, fechas inclusivas y estado de cobro, en páginas de 25")
    public List<Row> history(@RequestParam Kind kind, @RequestParam(required = false) String plate,
                             @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                             @RequestParam(required = false) ChargeStatus status, @RequestParam(defaultValue = "0") int page) {
        if (page < 0 || (from != null && to != null && from.isAfter(to)) || (kind == Kind.EVENTOS && status != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros inválidos.");
        }
        String source = kind == Kind.EVENTOS ? """
                SELECT e.id, s.nombre AS site, placa, ocurrido_en AS fecha, tipo_evento AS tipo,
                       resultado AS estado, motivo, NULL::numeric AS valor FROM eventos e JOIN sedes s ON s.id=e.sede_id
                """ : """
                SELECT c.id, s.nombre AS site, placa, generado_en AS fecha, 'COBRO' AS tipo,
                       estado, NULL::text AS motivo, valor FROM cobros c JOIN sedes s ON s.id=c.sede_id
                """;
        var sql = new StringBuilder("SELECT * FROM (" + source + ") movimientos WHERE TRUE");
        var params = new HashMap<String, Object>();
        if (plate != null && !plate.isBlank()) {
            sql.append(" AND placa = :plate");
            params.put("plate", new PlateClassifier().classify(plate).plate());
        }
        if (from != null) {
            sql.append(" AND fecha >= :start");
            params.put("start", from.atStartOfDay(parkingZone).toOffsetDateTime());
        }
        if (to != null) {
            sql.append(" AND fecha < :end");
            params.put("end", to.plusDays(1).atStartOfDay(parkingZone).toOffsetDateTime());
        }
        if (status != null) {
            sql.append(" AND estado = :status");
            params.put("status", status.name());
        }
        sql.append(" ORDER BY fecha DESC, id DESC LIMIT 25 OFFSET :offset");
        params.put("offset", page * 25L);
        return jdbc.sql(sql.toString()).params(params).query((row, index) -> new Row(row.getLong("id"),
                row.getString("site"), row.getString("placa"), row.getObject("fecha", OffsetDateTime.class),
                row.getString("tipo"), row.getString("estado"), row.getString("motivo"), row.getBigDecimal("valor"))).list();
    }
}
