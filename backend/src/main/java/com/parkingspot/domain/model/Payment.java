package com.parkingspot.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Pago registrado para un cobro, con su trazabilidad (HU-16 CA-03). */
public record Payment(
        Long id,
        long chargeId,
        PaymentMethod method,
        BigDecimal amount,
        String operator,
        String idempotencyKey,
        OffsetDateTime registeredAt) {

    public static Payment cash(long chargeId, BigDecimal amount, String operator, String idempotencyKey,
                               OffsetDateTime registeredAt) {
        return new Payment(null, chargeId, PaymentMethod.EFECTIVO, amount, operator, idempotencyKey, registeredAt);
    }

    /** Una repetición de la solicitud trae el mismo cobro y el mismo importe. */
    public boolean sameRequestAs(long otherChargeId, BigDecimal otherAmount) {
        return chargeId == otherChargeId && amount.compareTo(otherAmount) == 0;
    }
}
