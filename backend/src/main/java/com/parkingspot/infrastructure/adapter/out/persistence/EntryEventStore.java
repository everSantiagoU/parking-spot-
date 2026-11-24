package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.Confidence;
import com.parkingspot.domain.model.EntryEvent;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.port.out.EntryEventRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Guarda los eventos de ingreso en la tabla eventos (HU-05) con JdbcClient, igual que AccountStore.
 * Traduce el dominio al esquema sin cambiar el contrato de la HU-08:
 * SIMULADOR → AUTOMATICA, MANUAL → MANUAL, y la confianza de 0 a 1 → 0 a 100.
 */
@Repository
public class EntryEventStore implements EntryEventRepository {
    private final JdbcClient jdbc;

    public EntryEventStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long save(EntryEvent event) {
        var keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO eventos (sede_id, placa, tipo_vehiculo, tipo_evento, origen_lectura, confianza,
                                     resultado, motivo, usuario_id, ocurrido_en)
                VALUES (:sede, :placa, :tipo, 'INGRESO', :origen, :confianza, :resultado, :motivo, :usuario, :ocurrido)
                """)
                .param("sede", event.siteId())
                .param("placa", event.plate())
                .param("tipo", event.type().name())
                .param("origen", readingOrigin(event.source()))
                .param("confianza", percentage(event.confidence()), Types.NUMERIC)
                .param("resultado", event.result().name())
                .param("motivo", event.reason(), Types.VARCHAR)
                .param("usuario", event.userId())
                .param("ocurrido", OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC),
                        Types.TIMESTAMP_WITH_TIMEZONE)
                .update(keys);
        return generatedId(keys);
    }

    /** eventos.origen_lectura solo admite AUTOMATICA o MANUAL. */
    static String readingOrigin(ReadingSource source) {
        return switch (source) {
            case MANUAL -> "MANUAL";
            case SIMULADOR -> "AUTOMATICA";
        };
    }

    /** eventos.confianza es NUMERIC(5,2) de 0 a 100. */
    static BigDecimal percentage(Confidence confidence) {
        if (confidence == null) {
            return null;
        }
        return BigDecimal.valueOf(confidence.value()).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
    }

    /** PostgreSQL devuelve todas las columnas y H2 solo la identidad: se busca "id" sin importar mayúsculas. */
    private static long generatedId(GeneratedKeyHolder keys) {
        return keys.getKeyList().get(0).entrySet().stream()
                .filter(column -> column.getKey().equalsIgnoreCase("id"))
                .map(column -> ((Number) column.getValue()).longValue())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La base de datos no devolvió el id del evento."));
    }
}
