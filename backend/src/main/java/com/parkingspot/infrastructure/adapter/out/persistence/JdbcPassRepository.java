package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.Pass;
import com.parkingspot.domain.port.out.PassRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** El pase es el cobro PAGADO con su vigente_hasta (restricción ck_cobros_pase_pagado de HU-05). */
@Repository
public class JdbcPassRepository implements PassRepository {
    private final JdbcClient jdbc;

    public JdbcPassRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Pass> findLatest(String plate, long preferredSiteId) {
        return jdbc.sql("""
                        SELECT id, placa, sede_id, dia_operativo, vigente_hasta
                        FROM cobros
                        WHERE placa = :plate AND estado = 'PAGADO' AND vigente_hasta IS NOT NULL
                        ORDER BY (sede_id = :siteId) DESC, vigente_hasta DESC
                        LIMIT 1
                        """)
                .param("plate", plate)
                .param("siteId", preferredSiteId)
                .query((row, index) -> new Pass(
                        row.getLong("id"),
                        row.getString("placa"),
                        row.getLong("sede_id"),
                        row.getObject("dia_operativo", LocalDate.class),
                        row.getObject("vigente_hasta", OffsetDateTime.class)))
                .optional();
    }
}
