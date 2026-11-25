package com.parkingspot.domain.model;

/**
 * Datos crudos que recibe un lector de placas. Todavía no están validados:
 * el texto puede tener un formato inválido y la confianza puede faltar o estar fuera de rango.
 */
public record PlateCapture(String plateText, Double confidence) {
}
