package com.parkingspot.application.usecase;

import com.parkingspot.domain.model.EntryAttempt;
import com.parkingspot.domain.model.PlateCapture;

/** Registra el ingreso de un vehículo en la sede del operador autenticado (HU-10). */
public interface RegisterEntryUseCase {
    /** Lectura del lector: solo se registra si no requiere validación (HU-11). */
    EntryAttempt enterAutomatic(PlateCapture capture, String operatorUsername);

    /** Placa digitada o confirmada por el operador: se registra con origen MANUAL. */
    EntryAttempt enterManual(String typedPlate, String operatorUsername);
}
