package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.RawPlateReading;

/**
 * Puerto de salida hacia un lector de placas. El simulador lo implementa hoy;
 * una cámara real podría implementarlo después sin cambiar el dominio ni la aplicación.
 */
public interface PlateReaderPort {
    RawPlateReading read(PlateCapture capture);
}
