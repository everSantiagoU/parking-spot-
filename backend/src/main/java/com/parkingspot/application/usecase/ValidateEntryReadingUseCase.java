package com.parkingspot.application.usecase;

import com.parkingspot.domain.model.AcceptedReading;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.ReadingEvaluation;

/** Valida una lectura antes del ingreso (HU-11). */
public interface ValidateEntryReadingUseCase {
    /** Evalúa la lectura del lector contra el umbral y el formato de placa. */
    ReadingEvaluation validateAutomatic(PlateCapture capture, String operatorUsername);

    /** El operador digita o confirma la placa: la lectura queda MANUAL y con su usuario. */
    AcceptedReading confirmManual(String typedPlate, String operatorUsername);
}
