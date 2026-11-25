package com.parkingspot.domain.model;

/** Resultado que ve el operador en el carril de entrada. */
public enum EntryOutcome {
    /** Se registró el ingreso y la barrera se abre. */
    AUTORIZADO,
    /** La lectura necesita que el operador digite o confirme la placa; no se registra la entrada. */
    REQUIERE_VALIDACION,
    /** No hay cupo para ese tipo de vehículo: la barrera sigue cerrada y el evento queda RECHAZADO (HU-13). */
    LLENO,
    INGRESO_ACTIVO
}
