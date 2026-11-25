package com.parkingspot.domain.model;

/** Decisión del carril. Una autorización permite simular la apertura. */
public enum ExitDecision {
    PAGO_CONFIRMADO,
    EXENCION_COMPROBADA,
    PAGO_PENDIENTE,
    COBRO_NO_SATISFECHO,
    EXENCION_INDETERMINADA,
    SIN_INGRESO_ACTIVO,
    ERROR_CONSULTA,
    OCUPACION_INCONSISTENTE,
    ERROR_REGISTRO_SALIDA,
    ERROR_AUDITORIA;

    public boolean authorizesOpening() {
        return this == PAGO_CONFIRMADO || this == EXENCION_COMPROBADA;
    }
}
