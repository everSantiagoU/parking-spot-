package com.parkingspot.application.service;

import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.usecase.ValidateExitUseCase;
import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.model.ExitDecision;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.port.out.ChargeRepository;
import com.parkingspot.domain.port.out.ExitBarrier;
import com.parkingspot.domain.port.out.ExitEventRepository;
import com.parkingspot.domain.port.out.ExitEventRepository.ActiveEntry;
import com.parkingspot.domain.port.out.SpotRelease;
import com.parkingspot.domain.port.out.OperatorRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExitValidationService implements ValidateExitUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExitValidationService.class);

    private final PlateClassifier plates;
    private final OperatorRepository operators;
    private final ExitEventRepository events;
    private final ChargeRepository charges;
    private final OperatingDayCalculator operatingDays;
    private final ExitBarrier barrier;
    private final Clock clock;
    private final SpotRelease occupancy;
    private final Transactions transactions;

    public ExitValidationService(PlateClassifier plates, OperatorRepository operators, ExitEventRepository events,
                                 ChargeRepository charges, OperatingDayCalculator operatingDays, ExitBarrier barrier,
                                 Clock clock, SpotRelease occupancy, Transactions transactions) {
        this.plates = plates;
        this.operators = operators;
        this.events = events;
        this.charges = charges;
        this.operatingDays = operatingDays;
        this.barrier = barrier;
        this.clock = clock;
        this.occupancy = occupancy;
        this.transactions = transactions;
    }

    @Override
    public ExitDecision validate(String plate, String operatorUsername) {
        var vehicle = plates.classify(plate);
        var now = clock.instant();
        Operator operator;
        try {
            operator = operators.findActiveOperator(operatorUsername).orElse(null);
        } catch (RuntimeException exception) {
            log.error("Error al consultar operador {} para la salida de {}", operatorUsername, vehicle.plate(),
                    exception);
            return ExitDecision.ERROR_CONSULTA;
        }
        if (operator == null) {
            throw new OperatorNotAvailableException();
        }

        var attempt = new Attempt(operator, vehicle, now);
        Evaluation evaluation;
        try {
            evaluation = evaluate(attempt);
        } catch (RuntimeException exception) {
            logFailure(attempt, ExitDecision.ERROR_CONSULTA, exception);
            return audit(attempt, ExitDecision.ERROR_CONSULTA, null);
        }

        if (!evaluation.decision().authorizesOpening()) {
            return audit(attempt, evaluation.decision(), evaluation.chargeId());
        }
        return completeExit(attempt, evaluation);
    }

    private ExitDecision completeExit(Attempt attempt, Evaluation evaluation) {
        ExitDecision committed;
        try {
            committed = transactions.inTransaction(() -> persistExit(attempt, evaluation));
        } catch (ExitWriteFailure failure) {
            logFailure(attempt, failure.decision, failure);
            return failure.decision == ExitDecision.ERROR_AUDITORIA ? failure.decision
                    : audit(attempt, failure.decision, evaluation.chargeId());
        } catch (RuntimeException exception) {
            logFailure(attempt, ExitDecision.ERROR_REGISTRO_SALIDA, exception);
            return audit(attempt, ExitDecision.ERROR_REGISTRO_SALIDA, evaluation.chargeId());
        }

        if (committed.authorizesOpening()) {
            barrier.open(attempt.operator().siteId(), attempt.vehicle().plate());
        }
        return committed;
    }

    private ExitDecision persistExit(Attempt attempt, Evaluation evaluation) {
        var entry = evaluation.entry();
        var chargeId = evaluation.chargeId();
        var decision = evaluation.decision();
        if (!events.recordEffectiveExit(attempt.operator(), entry, attempt.now(), decision, chargeId)) {
            // Otra solicitud consumió este mismo ingreso. El intento se registra sin liberar otro cupo.
            return recordAttempt(attempt, ExitDecision.SIN_INGRESO_ACTIVO, null);
        }

        if (!occupancy.releaseOccupiedSpot(attempt.operator().siteId(), entry.type())) {
            throw new ExitWriteFailure(ExitDecision.OCUPACION_INCONSISTENTE, null);
        }
        return recordAttempt(attempt, decision, chargeId);
    }

    private ExitDecision audit(Attempt attempt, ExitDecision decision, Long chargeId) {
        try {
            return recordAttempt(attempt, decision, chargeId);
        } catch (ExitWriteFailure failure) {
            logFailure(attempt, failure.decision, failure);
            return failure.decision;
        }
    }

    private ExitDecision recordAttempt(Attempt attempt, ExitDecision decision, Long chargeId) {
        try {
            events.recordAttempt(attempt.operator(), attempt.vehicle(), attempt.now(), decision, chargeId);
            return decision;
        } catch (RuntimeException exception) {
            throw new ExitWriteFailure(ExitDecision.ERROR_AUDITORIA, exception);
        }
    }

    private static void logFailure(Attempt attempt, ExitDecision decision, RuntimeException exception) {
        log.error("{} al procesar salida de {} en sede {} por {} a las {}", decision, attempt.vehicle().plate(),
                attempt.operator().siteId(), attempt.operator().username(), attempt.now(), exception);
    }

    private Evaluation evaluate(Attempt attempt) {
        long siteId = attempt.operator().siteId();
        String plate = attempt.vehicle().plate();
        var entry = events.findActiveEntry(siteId, plate).orElse(null);
        if (entry == null) {
            return new Evaluation(ExitDecision.SIN_INGRESO_ACTIVO, null, null);
        }

        var day = operatingDays.operatingDayOf(attempt.now());
        var charge = charges.findByPlateAndSiteAndDay(plate, siteId, day);
        if (charge.isPresent()) {
            var current = charge.get();
            var decision = switch (current.status()) {
                case PAGADO -> ExitDecision.PAGO_CONFIRMADO;
                case PENDIENTE -> ExitDecision.PAGO_PENDIENTE;
                case VENCIDO, ANULADO -> ExitDecision.COBRO_NO_SATISFECHO;
            };
            return new Evaluation(decision, current.id(), entry);
        }
        return new Evaluation(entry.isExemptOn(day) ? ExitDecision.EXENCION_COMPROBADA
                : ExitDecision.EXENCION_INDETERMINADA, null, entry);
    }

    private record Attempt(Operator operator, VehicleClassification vehicle, Instant now) {
    }

    private record Evaluation(ExitDecision decision, Long chargeId, ActiveEntry entry) {
    }

    private static final class ExitWriteFailure extends RuntimeException {
        private final ExitDecision decision;

        private ExitWriteFailure(ExitDecision decision, Throwable cause) {
            super(decision.name(), cause);
            this.decision = decision;
        }
    }
}
