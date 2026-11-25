package com.parkingspot.domain.model;

/** Estado de un pase al momento de consultarlo (HU-17). */
public enum PassStatus {
    /** El día operativo del pase no ha terminado: el reingreso no genera cobro (CA-02 y CA-04). */
    VIGENTE,
    /** Ya pasaron las 06:00 del día siguiente al día operativo del pase (CA-03). */
    VENCIDO,
    /** El pase es de otra sede: no habilita la exención del cobro (CA-05). */
    OTRA_SEDE
}
