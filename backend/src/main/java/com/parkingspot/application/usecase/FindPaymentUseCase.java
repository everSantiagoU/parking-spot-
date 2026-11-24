package com.parkingspot.application.usecase;

import com.parkingspot.domain.model.Payment;

/** HU-16 CA-03: consulta el pago de un cobro. */
public interface FindPaymentUseCase {
    Payment findByCharge(long chargeId);
}
