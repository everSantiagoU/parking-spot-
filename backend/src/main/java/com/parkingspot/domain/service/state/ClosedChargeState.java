package com.parkingspot.domain.service.state;

import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.domain.exception.PaymentRejectedException.Reason;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;

/** Un cobro VENCIDO o ANULADO ya no se puede pagar. */
public class ClosedChargeState implements ChargeState {
    @Override
    public ChargeStatus pay(Charge charge) {
        throw new PaymentRejectedException(Reason.COBRO_NO_PAGABLE,
                "El cobro " + charge.id() + " está " + charge.status() + " y no se puede pagar.");
    }
}
