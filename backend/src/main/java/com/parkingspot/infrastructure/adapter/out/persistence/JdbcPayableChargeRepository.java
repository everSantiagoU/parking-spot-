package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.port.out.PayableChargeRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPayableChargeRepository implements PayableChargeRepository {
    private final JdbcClient jdbc;

    public JdbcPayableChargeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** FOR UPDATE: otra transacción que intente pagar el mismo cobro espera a que esta termine. */
    @Override
    public Optional<Charge> lockById(long chargeId) {
        return jdbc.sql("SELECT " + JdbcChargeRepository.COLUMNS + " FROM cobros WHERE id = :id FOR UPDATE")
                .param("id", chargeId)
                .query((row, index) -> JdbcChargeRepository.toCharge(row))
                .optional();
    }

    @Override
    public void updateStatus(long chargeId, ChargeStatus status, OffsetDateTime validUntil) {
        jdbc.sql("UPDATE cobros SET estado = :status, vigente_hasta = :validUntil WHERE id = :id")
                .param("status", status.name())
                .param("validUntil", validUntil)
                .param("id", chargeId)
                .update();
    }
}
