package com.parkingspot.application.dto;

import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.Payment;
import java.time.OffsetDateTime;

/**
 * Resultado de registrar un pago.
 * replayed es true cuando la solicitud repetía una clave de idempotencia ya procesada.
 */
public record PaymentResult(Payment payment, Charge charge, ChargeStatus chargeStatus, OffsetDateTime passValidUntil,
                            boolean replayed) {
}
