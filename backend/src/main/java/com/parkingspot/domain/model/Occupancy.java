package com.parkingspot.domain.model;

public record Occupancy(VehicleType vehicleType, int capacity, int occupied) {
}
