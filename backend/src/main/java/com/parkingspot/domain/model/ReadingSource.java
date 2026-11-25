package com.parkingspot.domain.model;

/**
 * Origen de una lectura de placa. Cada adaptador de lector declara su propia fuente;
 * MANUAL identifica una placa digitada o confirmada por el operador (HU-11).
 */
public enum ReadingSource {
    SIMULADOR,
    MANUAL
}
