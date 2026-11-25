package com.parkingspot.domain.model;

/** Resultado de validar una lectura antes del ingreso (HU-11). */
public enum ValidationStatus {
    /** La lectura puede continuar al ingreso. */
    ACEPTADA,
    /** El operador debe digitar o confirmar la placa; no se registra la entrada. */
    REQUIERE_VALIDACION
}
