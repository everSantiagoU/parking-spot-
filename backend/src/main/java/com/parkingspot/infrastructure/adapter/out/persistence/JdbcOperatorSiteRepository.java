package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.port.out.OperatorSiteRepository;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperatorSiteRepository implements OperatorSiteRepository {
    private final JdbcClient jdbc;

    public JdbcOperatorSiteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Long> findSiteOf(String username) {
        return jdbc.sql("SELECT sede_id FROM usuarios WHERE username = :username AND sede_id IS NOT NULL")
                .param("username", username)
                .query(Long.class)
                .optional();
    }
}
