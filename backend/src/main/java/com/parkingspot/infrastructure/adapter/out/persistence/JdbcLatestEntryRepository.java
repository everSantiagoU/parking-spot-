package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.port.out.LatestEntryRepository;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Última placa con evento INGRESO AUTORIZADO en la sede. */
@Repository
public class JdbcLatestEntryRepository implements LatestEntryRepository {
    private final JdbcClient jdbc;

    public JdbcLatestEntryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> findLatestAuthorizedPlate(long siteId) {
        return jdbc.sql("""
                        SELECT placa FROM eventos
                        WHERE sede_id = :site AND tipo_evento = 'INGRESO' AND resultado = 'AUTORIZADO'
                        ORDER BY ocurrido_en DESC, id DESC
                        LIMIT 1
                        """)
                .param("site", siteId)
                .query(String.class)
                .optional();
    }
}
