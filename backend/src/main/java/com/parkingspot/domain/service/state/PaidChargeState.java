package com.parkingspot.domain.service.state;

import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.domain.exception.PaymentRejectedException.Reason;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;

/** HU-16 CA-06: un cobro pagado no admite un segundo pago. */
public class PaidChargeState implements ChargeState {
    @Override
    public ChargeStatus pay(Charge charge) {
        throw new PaymentRejectedException(Reason.COBRO_YA_PAGADO,
                "El cobro " + charge.id() + " de la placa " + charge.plate() + " ya está pagado.");
    }
}
