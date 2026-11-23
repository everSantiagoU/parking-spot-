package com.parkingspot.application.dto;

import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.model.VehicleType;
import java.time.LocalDate;

/**
 * Resultado de calcular el cobro de un ingreso.
 * charge es null cuando el vehículo no genera obligación de pago (EXENTO).
 */
public record ChargeResult(
        String plate,
        VehicleType vehicleType,
        LocalDate operatingDay,
        TimeSlot timeSlot,
        Outcome outcome,
        Charge charge) {

    public enum Outcome {
        /** Se generó un cobro nuevo. */
        CREADO,
        /** La placa ya tenía cobro en este día operativo (reingreso). */
        EXISTENTE,
        /** El vehículo no genera obligación de pago (moto con tarifa $0). */
        EXENTO
    }

    public static ChargeResult created(Charge charge) {
        return fromCharge(charge, Outcome.CREADO);
    }

    public static ChargeResult existing(Charge charge) {
        return fromCharge(charge, Outcome.EXISTENTE);
    }

    public static ChargeResult exempt(VehicleClassification vehicle, LocalDate operatingDay, TimeSlot timeSlot) {
        return new ChargeResult(vehicle.plate(), vehicle.type(), operatingDay, timeSlot, Outcome.EXENTO, null);
    }

    private static ChargeResult fromCharge(Charge charge, Outcome outcome) {
        return new ChargeResult(charge.plate(), charge.vehicleType(), charge.operatingDay(), charge.timeSlot(),
                outcome, charge);
    }
}
