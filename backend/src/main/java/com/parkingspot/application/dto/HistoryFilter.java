package com.parkingspot.application.dto;

import java.time.LocalDate;

/** Filtros del historial tal como llegan de la petición; cualquiera puede venir en null. */
public record HistoryFilter(String plate, LocalDate from, LocalDate to, Integer page, Integer size) {
}
