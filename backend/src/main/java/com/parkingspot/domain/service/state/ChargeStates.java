package com.parkingspot.domain.service.state;

import com.parkingspot.domain.model.ChargeStatus;
import java.util.EnumMap;
import java.util.Map;

/** Devuelve el objeto de estado que corresponde a cada ChargeStatus. */
public class ChargeStates {
    private final Map<ChargeStatus, ChargeState> states = new EnumMap<>(ChargeStatus.class);

    public ChargeStates() {
        ChargeState closed = new ClosedChargeState();
        states.put(ChargeStatus.PENDIENTE, new PendingChargeState());
        states.put(ChargeStatus.PAGADO, new PaidChargeState());
        states.put(ChargeStatus.VENCIDO, closed);
        states.put(ChargeStatus.ANULADO, closed);
    }

    public ChargeState of(ChargeStatus status) {
        return states.get(status);
    }
}
