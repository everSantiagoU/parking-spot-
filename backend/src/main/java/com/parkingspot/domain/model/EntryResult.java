package com.parkingspot.domain.model;

/** Resultado que queda guardado en el evento de ingreso (eventos.resultado). */
public enum EntryResult {
    /** La entrada se autorizó y la barrera se abre. */
    AUTORIZADO,
    /** La entrada se rechazó; el motivo queda en el evento. */
    RECHAZADO
}
