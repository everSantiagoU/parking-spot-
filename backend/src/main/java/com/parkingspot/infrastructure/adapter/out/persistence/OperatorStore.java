package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.port.out.OperatorRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Lee el operador autenticado y su sede desde la tabla usuarios (HU-05/HU-06), igual que AccountStore. */
@Repository
public class OperatorStore implements OperatorRepository {
    private final JdbcClient jdbc;

    public OperatorStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Operator> findActiveOperator(String username) {
        return jdbc.sql("""
                SELECT u.id, u.username, u.sede_id
                FROM usuarios u
                JOIN roles r ON r.id = u.rol_id
                JOIN sedes s ON s.id = u.sede_id
                WHERE u.username = :username AND u.activo AND r.nombre = 'OPERADOR' AND s.activa
                """)
                .param("username", username)
                .query((row, index) -> new Operator(row.getLong("id"), row.getString("username"),
                        row.getLong("sede_id")))
                .optional();
    }
}
