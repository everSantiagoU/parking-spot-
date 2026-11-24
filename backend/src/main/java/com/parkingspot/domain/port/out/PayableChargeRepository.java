package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import java.time.OffsetDateTime;
import java.util.Optional;

/** Operaciones sobre un cobro durante su pago. */
public interface PayableChargeRepository {

    /** Lee el cobro y lo bloquea hasta el final de la transacción, para que dos pagos no compitan. */
    Optional<Charge> lockById(long chargeId);

    /** Cambia el estado del cobro y deja el pase vigente hasta validUntil (exclusivo). */
    void updateStatus(long chargeId, ChargeStatus status, OffsetDateTime validUntil);
}
