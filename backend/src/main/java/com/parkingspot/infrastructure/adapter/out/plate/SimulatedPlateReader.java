package com.parkingspot.infrastructure.adapter.out.plate;

import com.parkingspot.domain.model.Confidence;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.RawPlateReading;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.port.out.PlateReaderPort;

/**
 * Lector de placas simulado: devuelve la placa y la confianza que envía el operador,
 * en lugar de leerlas de una cámara física. Marca la lectura con la fuente SIMULADOR.
 */
public class SimulatedPlateReader implements PlateReaderPort {
    @Override
    public RawPlateReading read(PlateCapture capture) {
        return new RawPlateReading(
                capture.plateText(),
                Confidence.of(capture.confidence()),
                ReadingSource.SIMULADOR);
    }
}
