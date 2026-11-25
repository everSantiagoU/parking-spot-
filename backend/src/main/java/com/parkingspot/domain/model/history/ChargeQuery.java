package com.parkingspot.domain.model.history;

import com.parkingspot.domain.model.ChargeStatus;
import java.time.LocalDate;

/**
 * Filtros ya validados para el historial de cobros. Los campos en null no filtran;
 * siteId en null significa todas las sedes. Las fechas se comparan con el día operativo (inclusive).
 */
public record ChargeQuery(Long siteId, String plate, ChargeStatus status, LocalDate from, LocalDate to,
                          int page, int size) {
}
