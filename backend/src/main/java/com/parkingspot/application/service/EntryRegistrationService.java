package com.parkingspot.application.service;

import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.usecase.RegisterEntryUseCase;
import com.parkingspot.application.usecase.ValidateEntryReadingUseCase;
import com.parkingspot.domain.model.AcceptedReading;
import com.parkingspot.domain.model.EntryAttempt;
import com.parkingspot.domain.model.EntryEvent;
import com.parkingspot.domain.model.EntryOutcome;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.port.out.EntryEventRepository;
import com.parkingspot.domain.port.out.ExitEventRepository;
import com.parkingspot.domain.port.out.OccupancyRepository;

import com.parkingspot.domain.model.VehicleClassification;
import java.time.Clock;

/**
 * Registra el ingreso: valida la lectura (HU-11) y, si es una lectura aceptada, valida el cupo (HU-13),
 * guarda el evento y actualiza la ocupación en una sola transacción (HU-10).
 */
public class EntryRegistrationService implements RegisterEntryUseCase {
    private final ValidateEntryReadingUseCase validation;
    private final EntryEventRepository events;
    private final OccupancyRepository occupancy;
    private final Transactions transactions;
    private final Clock clock;
    private final ChargeService charges;
    private final ExitEventRepository exits;

    public EntryRegistrationService(ValidateEntryReadingUseCase validation, EntryEventRepository events,
                                    OccupancyRepository occupancy, Transactions transactions, Clock clock,
                                    ChargeService charges, ExitEventRepository exits) {
        this.exits = exits;
        this.validation = validation;
        this.events = events;
        this.occupancy = occupancy;
        this.transactions = transactions;
        this.clock = clock;
        this.charges = charges;
    }

    @Override
    public EntryAttempt enterAutomatic(PlateCapture capture, String operatorUsername) {
        var evaluation = validation.validateAutomatic(capture, operatorUsername);
        return evaluation.acceptedReading()
                .map(accepted -> register(evaluation.readText(), accepted))
                // CA-04 de la HU-11: sin lectura aceptada no se guarda evento ni cambia la ocupación.
                .orElseGet(() -> EntryAttempt.requiresValidation(evaluation, operatorUsername));
    }

    @Override
    public EntryAttempt enterManual(String typedPlate, String operatorUsername) {
        return register(typedPlate, validation.confirmManual(typedPlate, operatorUsername));
    }

    private EntryAttempt register(String readText, AcceptedReading accepted) {
        // HU-10 CA-02: la hora sale del reloj del servidor, nunca de la petición.
        var operator = accepted.operator();
        return transactions.inTransaction(() -> {
            occupancy.lockForEntry(operator.siteId(), accepted.type());
            var now = clock.instant();
            if (exits.findActiveEntry(operator.siteId(), accepted.plate()).isPresent()) {
                var rejected = EntryEvent.rejectedActive(accepted, now);
                return EntryAttempt.registered(EntryOutcome.INGRESO_ACTIVO, readText, rejected,
                        events.save(rejected, null), operator.username());
            }
            // HU-13 CA-01 y CA-04: el cupo se valida por sede y tipo de vehículo, y se ocupa en la misma
            // operación atómica, así que ingresos simultáneos no superan la capacidad.
            if (occupancy.occupySpotIfAvailable(operator.siteId(), accepted.type())) {
                var authorized = EntryEvent.authorized(accepted, now);
                var exemption = charges.settleEntryObligation(
                        new VehicleClassification(accepted.plate(), accepted.type()), operator.siteId(), now);
                return EntryAttempt.registered(EntryOutcome.AUTORIZADO, readText, authorized,
                        events.save(authorized, exemption.orElse(null)), operator.username());
            }
            // HU-13 CA-02 y CA-03: sin cupo → LLENO, la barrera sigue cerrada y el intento queda RECHAZADO.
            var rejected = EntryEvent.rejectedFull(accepted, now);
            return EntryAttempt.registered(EntryOutcome.LLENO, readText, rejected, events.save(rejected, null),
                    operator.username());
        });
    }
}
