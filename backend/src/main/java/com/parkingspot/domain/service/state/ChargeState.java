package com.parkingspot.domain.service.state;

import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;

/**
 * Patrón State: el comportamiento de un cobro ante un pago depende de su estado.
 * PENDIENTE → PAGADO es la única transición válida; los demás estados rechazan el pago.
 */
public interface ChargeState {

    /** Devuelve el estado siguiente tras pagar, o lanza un rechazo si el estado no lo permite. */
    ChargeStatus pay(Charge charge);
}
