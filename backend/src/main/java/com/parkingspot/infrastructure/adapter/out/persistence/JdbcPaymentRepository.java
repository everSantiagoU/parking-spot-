package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.domain.exception.PaymentRejectedException.Reason;
import com.parkingspot.domain.model.Payment;
import com.parkingspot.domain.model.PaymentMethod;
import com.parkingspot.domain.port.out.PaymentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {
    private static final String SELECT = """
            SELECT p.id, p.cobro_id, p.medio, p.valor, u.username, p.clave_idempotencia, p.registrado_en
            FROM pagos p JOIN usuarios u ON u.id = p.usuario_id
            """;

    private final JdbcClient jdbc;

    public JdbcPaymentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jdbc.sql(SELECT + " WHERE p.clave_idempotencia = :key")
                .param("key", idempotencyKey)
                .query((row, index) -> toPayment(row))
                .optional();
    }

    @Override
    public Optional<Payment> findByChargeId(long chargeId) {
        return jdbc.sql(SELECT + " WHERE p.cobro_id = :chargeId")
                .param("chargeId", chargeId)
                .query((row, index) -> toPayment(row))
                .optional();
    }

    /**
     * Las restricciones uq_pagos_cobro y uq_pagos_clave_idempotencia (HU-05) son la última defensa:
     * si se violan, se responde con el mismo error controlado que da el servicio.
     */
    @Override
    public Payment save(Payment payment) {
        try {
            long id = jdbc.sql("""
                            INSERT INTO pagos (cobro_id, medio, valor, usuario_id, clave_idempotencia, registrado_en)
                            VALUES (:chargeId, :method, :amount,
                                    (SELECT id FROM usuarios WHERE username = :operator),
                                    :key, :registeredAt)
                            RETURNING id
                            """)
                    .param("chargeId", payment.chargeId())
                    .param("method", payment.method().name())
                    .param("amount", payment.amount())
                    .param("operator", payment.operator())
                    .param("key", payment.idempotencyKey())
                    .param("registeredAt", payment.registeredAt())
                    .query(Long.class)
                    .single();
            return new Payment(id, payment.chargeId(), payment.method(), payment.amount(), payment.operator(),
                    payment.idempotencyKey(), payment.registeredAt());
        } catch (DuplicateKeyException duplicate) {
            String detail = String.valueOf(duplicate.getMessage());
            if (detail.contains("uq_pagos_clave_idempotencia")) {
                throw new PaymentRejectedException(Reason.IDEMPOTENCIA_CONFLICTO,
                        "La clave de idempotencia ya se usó con otros datos de pago.");
            }
            throw new PaymentRejectedException(Reason.COBRO_YA_PAGADO, "El cobro ya está pagado.");
        }
    }

    private static Payment toPayment(ResultSet row) throws SQLException {
        return new Payment(
                row.getLong("id"),
                row.getLong("cobro_id"),
                PaymentMethod.valueOf(row.getString("medio")),
                row.getBigDecimal("valor"),
                row.getString("username"),
                row.getString("clave_idempotencia"),
                row.getObject("registrado_en", OffsetDateTime.class));
    }
}
