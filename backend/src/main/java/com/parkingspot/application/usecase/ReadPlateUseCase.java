package com.parkingspot.application.usecase;

import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.PlateReading;

public interface ReadPlateUseCase {
    PlateReading read(PlateCapture capture);
}
