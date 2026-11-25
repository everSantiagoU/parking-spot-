package com.parkingspot.domain.model.history;

import com.parkingspot.domain.model.EventType;
import java.time.Instant;

/**
 * Filtros ya validados para el historial de eventos. Los campos en null no filtran;
 * siteId en null significa todas las sedes (administrador). El intervalo es [from, to).
 */
public record EventQuery(Long siteId, String plate, EventType type, Instant from, Instant to, int page, int size) {
}
