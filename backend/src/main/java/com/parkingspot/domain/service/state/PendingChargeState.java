package com.parkingspot.domain.service.state;

import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;

public class PendingChargeState implements ChargeState {
    @Override
    public ChargeStatus pay(Charge charge) {
        return ChargeStatus.PAGADO;
    }
}
