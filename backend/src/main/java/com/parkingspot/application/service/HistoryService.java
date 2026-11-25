package com.parkingspot.application.service;

import com.parkingspot.application.dto.HistoryFilter;
import com.parkingspot.application.usecase.QueryChargesUseCase;
import com.parkingspot.application.usecase.QueryEventsUseCase;
import com.parkingspot.domain.exception.InvalidHistoryFilterException;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.EventType;
import com.parkingspot.domain.model.history.ChargeQuery;
import com.parkingspot.domain.model.history.ChargeRecord;
import com.parkingspot.domain.model.history.EventQuery;
import com.parkingspot.domain.model.history.EventRecord;
import com.parkingspot.domain.model.history.PageResult;
import com.parkingspot.domain.port.out.ChargeHistoryRepository;
import com.parkingspot.domain.port.out.EventHistoryRepository;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Valida los filtros del historial y delega la consulta a los repositorios (HU-20). */
public class HistoryService implements QueryEventsUseCase, QueryChargesUseCase {
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;

    private final PlateClassifier plateClassifier;
    private final EventHistoryRepository events;
    private final ChargeHistoryRepository charges;
    private final ZoneId zone;

    public HistoryService(PlateClassifier plateClassifier, EventHistoryRepository events,
                          ChargeHistoryRepository charges, ZoneId zone) {
        this.plateClassifier = plateClassifier;
        this.events = events;
        this.charges = charges;
        this.zone = zone;
    }

    @Override
    public PageResult<EventRecord> findEvents(HistoryFilter filter, EventType type, Long siteId) {
        validateInterval(filter);
        return events.find(new EventQuery(siteId, plateOf(filter), type,
                startOf(filter.from()), startOf(nextDay(filter.to())), page(filter), size(filter)));
    }

    @Override
    public PageResult<ChargeRecord> findCharges(HistoryFilter filter, ChargeStatus status, Long siteId) {
        validateInterval(filter);
        return charges.find(new ChargeQuery(siteId, plateOf(filter), status,
                filter.from(), filter.to(), page(filter), size(filter)));
    }

    /** CP-HU-20-X01: un intervalo con la fecha inicial después de la final es un error de validación. */
    private static void validateInterval(HistoryFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new InvalidHistoryFilterException("La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    private String plateOf(HistoryFilter filter) {
        if (filter.plate() == null || filter.plate().isBlank()) {
            return null;
        }
        return plateClassifier.classify(filter.plate()).plate();
    }

    private static int page(HistoryFilter filter) {
        int page = filter.page() == null ? 0 : filter.page();
        if (page < 0) {
            throw new InvalidHistoryFilterException("La página no puede ser negativa.");
        }
        return page;
    }

    private static int size(HistoryFilter filter) {
        int size = filter.size() == null ? DEFAULT_SIZE : filter.size();
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidHistoryFilterException("El tamaño de página debe estar entre 1 y " + MAX_SIZE + ".");
        }
        return size;
    }

    private static LocalDate nextDay(LocalDate day) {
        return day == null ? null : day.plusDays(1);
    }

    /** Las fechas del filtro son días calendario en la zona de la sede. */
    private Instant startOf(LocalDate day) {
        return day == null ? null : day.atStartOfDay(zone).toInstant();
    }
}
