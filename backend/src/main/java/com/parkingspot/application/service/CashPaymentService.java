package com.parkingspot.application.service;

import com.parkingspot.application.dto.PaymentResult;
import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.usecase.FindPaymentUseCase;
import com.parkingspot.application.usecase.RegisterCashPaymentUseCase;
import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.domain.exception.PaymentRejectedException.Reason;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.Payment;
import com.parkingspot.domain.port.out.PayableChargeRepository;
import com.parkingspot.domain.port.out.PaymentRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.state.ChargeStates;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Pago en efectivo idempotente (HU-16).
 * Todo ocurre en una transacción: se bloquea el cobro, se revisa la clave de idempotencia,
 * el estado del cobro decide si se puede pagar (patrón State) y se valida el importe.
 */
public class CashPaymentService implements RegisterCashPaymentUseCase, FindPaymentUseCase {
    private static final int MAX_KEY_LENGTH = 64;

    private final PayableChargeRepository charges;
    private final PaymentRepository payments;
    private final ChargeStates states;
    private final OperatingDayCalculator operatingDays;
    private final Transactions transactions;
    private final Clock clock;

    public CashPaymentService(PayableChargeRepository charges, PaymentRepository payments, ChargeStates states,
                              OperatingDayCalculator operatingDays, Transactions transactions, Clock clock) {
        this.charges = charges;
        this.payments = payments;
        this.states = states;
        this.operatingDays = operatingDays;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public PaymentResult payInCash(long chargeId, BigDecimal amount, String idempotencyKey, String operator) {
        validateRequest(amount, idempotencyKey);
        return transactions.inTransaction(() -> {
            // CA-04: el cobro debe existir. El bloqueo evita que dos pagos del mismo cobro se crucen.
            Charge charge = charges.lockById(chargeId)
                    .orElseThrow(() -> new PaymentRejectedException(Reason.COBRO_NO_ENCONTRADO,
                            "No existe el cobro " + chargeId + "."));

            // CA-02 y CP-X02: se revisa la clave ya con el cobro bloqueado.
            Optional<Payment> previous = payments.findByIdempotencyKey(idempotencyKey);
            if (previous.isPresent()) {
                return replay(previous.get(), chargeId, amount, charge);
            }

            // CA-06: el estado del cobro decide si se puede pagar (patrón State).
            ChargeStatus next = states.of(charge.status()).pay(charge);

            // CA-05: el importe debe ser exactamente el valor pendiente.
            if (charge.amount().compareTo(amount) != 0) {
                throw new PaymentRejectedException(Reason.IMPORTE_INCORRECTO,
                        "El importe debe ser " + charge.amount().toPlainString() + ".");
            }

            OffsetDateTime now = operatingDays.localDateTimeOf(clock.instant());
            Payment saved = payments.save(Payment.cash(chargeId, amount, operator, idempotencyKey, now));
            // CA-08: el pase queda vigente hasta el final del día operativo del cobro.
            OffsetDateTime passValidUntil = operatingDays.endOf(charge.operatingDay());
            charges.updateStatus(chargeId, next, passValidUntil);
            return new PaymentResult(saved, charge, next, passValidUntil, false);
        });
    }

    @Override
    public Payment findByCharge(long chargeId) {
        return payments.findByChargeId(chargeId)
                .orElseThrow(() -> new PaymentRejectedException(Reason.PAGO_NO_ENCONTRADO,
                        "El cobro " + chargeId + " no tiene pagos registrados."));
    }

    private PaymentResult replay(Payment previous, long chargeId, BigDecimal amount, Charge charge) {
        if (!previous.sameRequestAs(chargeId, amount)) {
            throw new PaymentRejectedException(Reason.IDEMPOTENCIA_CONFLICTO,
                    "La clave de idempotencia ya se usó con otros datos de pago.");
        }
        return new PaymentResult(previous, charge, charge.status(), operatingDays.endOf(charge.operatingDay()), true);
    }

    private static void validateRequest(BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new PaymentRejectedException(Reason.SOLICITUD_INVALIDA,
                    "La clave de idempotencia es obligatoria y tiene máximo " + MAX_KEY_LENGTH + " caracteres.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentRejectedException(Reason.SOLICITUD_INVALIDA, "El importe debe ser mayor a $0.");
        }
    }
}
