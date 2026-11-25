package com.parkingspot.application.service;

import com.parkingspot.application.usecase.ReadPlateUseCase;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.PlateReading;
import com.parkingspot.domain.port.out.PlateReaderPort;
import com.parkingspot.domain.service.PlateClassifier;

/**
 * Obtiene una lectura del lector configurado y la normaliza y clasifica con PlateClassifier (HU-09).
 * Solo conoce el puerto PlateReaderPort, no el adaptador concreto.
 */
public class PlateReadingService implements ReadPlateUseCase {
    private final PlateReaderPort reader;
    private final PlateClassifier classifier;

    public PlateReadingService(PlateReaderPort reader, PlateClassifier classifier) {
        this.reader = reader;
        this.classifier = classifier;
    }

    @Override
    public PlateReading read(PlateCapture capture) {
        var raw = reader.read(capture);
        var classification = classifier.classify(raw.plateText());
        return new PlateReading(classification.plate(), classification.type(), raw.confidence(), raw.source());
    }
}
