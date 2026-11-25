package com.parkingspot.application.usecase;

import com.parkingspot.application.dto.PaymentResult;
import java.math.BigDecimal;

/** HU-16: registra el pago en efectivo de un cobro pendiente. */
public interface RegisterCashPaymentUseCase {
    PaymentResult payInCash(long chargeId, BigDecimal amount, String idempotencyKey, String operator);
}
