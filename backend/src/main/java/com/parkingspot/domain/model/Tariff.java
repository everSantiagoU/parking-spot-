package com.parkingspot.domain.model;

import java.math.BigDecimal;

/** Tarifa configurada para una sede, un tipo de vehículo y una franja. */
public record Tariff(long id, VehicleType vehicleType, TimeSlot timeSlot, BigDecimal value) {
}
