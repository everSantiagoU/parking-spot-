package com.parkingspot.application.service;

import com.parkingspot.application.usecase.ValidateEntryReadingUseCase;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.model.AcceptedReading;
import com.parkingspot.domain.model.ConfidenceThreshold;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.PlateReading;
import com.parkingspot.domain.model.ReadingEvaluation;
import com.parkingspot.domain.model.ValidationReason;
import com.parkingspot.domain.port.out.OperatorRepository;
import com.parkingspot.domain.port.out.PlateReaderPort;
import com.parkingspot.domain.service.PlateClassifier;

/**
 * Decide si una lectura puede continuar al ingreso:
 * formato inválido o confianza menor que el umbral → REQUIERE_VALIDACION; si no → ACEPTADA.
 * Reutiliza el lector de la HU-08 y PlateClassifier de la HU-09.
 */
public class EntryReadingValidationService implements ValidateEntryReadingUseCase {
    private final PlateReaderPort reader;
    private final PlateClassifier classifier;
    private final ConfidenceThreshold threshold;
    private final OperatorRepository operators;

    public EntryReadingValidationService(PlateReaderPort reader, PlateClassifier classifier,
                                         ConfidenceThreshold threshold, OperatorRepository operators) {
        this.reader = reader;
        this.classifier = classifier;
        this.threshold = threshold;
        this.operators = operators;
    }

    @Override
    public ReadingEvaluation validateAutomatic(PlateCapture capture, String operatorUsername) {
        var operator = operator(operatorUsername);
        var raw = reader.read(capture);
        PlateReading reading;
        try {
            var classification = classifier.classify(raw.plateText());
            reading = new PlateReading(classification.plate(), classification.type(), raw.confidence(), raw.source());
        } catch (InvalidPlateException invalidFormat) {
            return ReadingEvaluation.requiresValidation(ValidationReason.FORMATO_INVALIDO, raw.plateText(), null,
                    null, raw.confidence(), raw.source());
        }
        if (threshold.requiresValidation(reading.confidence())) {
            return ReadingEvaluation.requiresValidation(ValidationReason.CONFIANZA_BAJA, raw.plateText(),
                    reading.plate(), reading.type(), reading.confidence(), reading.source());
        }
        return ReadingEvaluation.accepted(raw.plateText(), AcceptedReading.automatic(reading, threshold, operator));
    }

    @Override
    public AcceptedReading confirmManual(String typedPlate, String operatorUsername) {
        var operator = operator(operatorUsername);
        return AcceptedReading.manual(classifier.classify(typedPlate), operator);
    }

    private Operator operator(String username) {
        return operators.findActiveOperator(username).orElseThrow(OperatorNotAvailableException::new);
    }
}
