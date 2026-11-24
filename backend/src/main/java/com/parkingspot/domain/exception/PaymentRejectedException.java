package com.parkingspot.domain.exception;

/** El pago no se puede registrar. El motivo permite responder con un error controlado. */
public class PaymentRejectedException extends RuntimeException {

    public enum Reason {
        SOLICITUD_INVALIDA,
        COBRO_NO_ENCONTRADO,
        IMPORTE_INCORRECTO,
        COBRO_YA_PAGADO,
        COBRO_NO_PAGABLE,
        IDEMPOTENCIA_CONFLICTO,
        PAGO_NO_ENCONTRADO
    }

    private final Reason reason;

    public PaymentRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
