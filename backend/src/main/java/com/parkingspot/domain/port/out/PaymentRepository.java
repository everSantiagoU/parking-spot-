package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Payment;
import java.util.Optional;

/** Persistencia de pagos. */
public interface PaymentRepository {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByChargeId(long chargeId);

    /** Guarda el pago y lo asocia al usuario operador por su nombre de usuario. */
    Payment save(Payment payment);
}
