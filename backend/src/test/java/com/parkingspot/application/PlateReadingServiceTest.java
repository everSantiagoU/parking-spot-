package com.parkingspot.application;

import com.parkingspot.application.service.PlateReadingService;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.model.Confidence;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.RawPlateReading;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.PlateReaderPort;
import com.parkingspot.domain.service.PlateClassifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlateReadingServiceTest {

    /** Lector falso que siempre devuelve el mismo texto, sin importar la captura recibida. */
    private static PlateReaderPort fakeReaderReturning(String plateText, double confidence) {
        return capture -> new RawPlateReading(plateText, new Confidence(confidence), ReadingSource.SIMULADOR);
    }

    @Test
    void normalizesAndClassifiesWhatTheReaderReturns() {
        var service = new PlateReadingService(fakeReaderReturning(" abc12d ", 0.93), new PlateClassifier());

        var reading = service.read(new PlateCapture(" abc12d ", 0.93));

        assertEquals("ABC12D", reading.plate());
        assertEquals(VehicleType.MOTO, reading.type());
        assertEquals(0.93, reading.confidence().value());
        assertEquals(ReadingSource.SIMULADOR, reading.source());
    }

    @Test
    void dependsOnlyOnThePortSoAnyReaderCanBePluggedIn() {
        // La captura dice AAA111, pero este lector devuelve otra placa:
        // el servicio usa lo que entrega el puerto, sin conocer la implementación.
        var service = new PlateReadingService(fakeReaderReturning("xyz987", 0.5), new PlateClassifier());

        var reading = service.read(new PlateCapture("AAA111", 0.99));

        assertEquals("XYZ987", reading.plate());
        assertEquals(VehicleType.CARRO, reading.type());
        assertEquals(0.5, reading.confidence().value());
    }

    @Test
    void propagatesInvalidPlateFromTheClassifier() {
        var service = new PlateReadingService(fakeReaderReturning("AB123", 0.9), new PlateClassifier());

        assertThrows(InvalidPlateException.class, () -> service.read(new PlateCapture("AB123", 0.9)));
    }
}
