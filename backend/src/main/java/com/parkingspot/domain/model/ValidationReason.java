package com.parkingspot.domain.model;

/** Por qué una lectura automática requiere validación del operador. */
public enum ValidationReason {
    /** La confianza es menor que el umbral configurado. */
    CONFIANZA_BAJA,
    /** El texto leído no tiene el formato de una placa de carro o moto. */
    FORMATO_INVALIDO
}
