package com.parkingspot.application.service;

import com.parkingspot.application.usecase.ValidateExitUseCase;
import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.model.ExitDecision;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.port.out.ChargeRepository;
import com.parkingspot.domain.port.out.ExitBarrier;
import com.parkingspot.domain.port.out.ExitEventRepository;
import com.parkingspot.domain.port.out.OperatorRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.Clock;
import java.time.LocalDate;
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

    public ExitValidationService(PlateClassifier plates, OperatorRepository operators, ExitEventRepository events,
                                 ChargeRepository charges, OperatingDayCalculator operatingDays, ExitBarrier barrier,
                                 Clock clock) {
        this.plates = plates;
        this.operators = operators;
        this.events = events;
        this.charges = charges;
        this.operatingDays = operatingDays;
        this.barrier = barrier;
        this.clock = clock;
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

        Evaluation evaluation;
        try {
            evaluation = evaluate(operator.siteId(), vehicle, operatingDays.operatingDayOf(now));
        } catch (RuntimeException exception) {
            log.error("Error al evaluar la salida de {} en sede {} por {}", vehicle.plate(), operator.siteId(),
                    operatorUsername, exception);
            evaluation = new Evaluation(ExitDecision.ERROR_CONSULTA, null);
        }

        var decision = evaluation.decision();
        try {
            events.recordAttempt(operator, vehicle, now, decision, evaluation.chargeId());
        } catch (RuntimeException exception) {
            log.error("Error al auditar la salida de {} en sede {} por {} a las {}. Decisión: {}",
                    vehicle.plate(), operator.siteId(), operatorUsername, now, decision, exception);
            return ExitDecision.ERROR_AUDITORIA;
        }

        if (decision.authorizesOpening()) {
            // HU-19: conectar la confirmación del paso con la transacción que cierra el ingreso y libera el cupo.
            barrier.open(operator.siteId(), vehicle.plate());
        }
        return decision;
    }

    private Evaluation evaluate(long siteId, VehicleClassification vehicle, LocalDate day) {
        var entry = events.findActiveEntry(siteId, vehicle.plate());
        if (entry.isEmpty()) {
            return new Evaluation(ExitDecision.SIN_INGRESO_ACTIVO, null);
        }

        var charge = charges.findByPlateAndSiteAndDay(vehicle.plate(), siteId, day);
        if (charge.isPresent()) {
            var current = charge.get();
            var decision = switch (current.status()) {
                case PAGADO -> ExitDecision.PAGO_CONFIRMADO;
                case PENDIENTE -> ExitDecision.PAGO_PENDIENTE;
                case VENCIDO, ANULADO -> ExitDecision.COBRO_NO_SATISFECHO;
            };
            return new Evaluation(decision, current.id());
        }
        return new Evaluation(entry.get().isExemptOn(day) ? ExitDecision.EXENCION_COMPROBADA
                : ExitDecision.EXENCION_INDETERMINADA, null);
    }

    private record Evaluation(ExitDecision decision, Long chargeId) {
    }
}
