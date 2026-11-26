package com.parkingspot.application.service;

import com.parkingspot.application.usecase.FindLatestEntryUseCase;
import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.port.out.LatestEntryRepository;
import com.parkingspot.domain.port.out.OperatorRepository;
import java.util.Optional;

/**
 * Busca la sede del operador y luego su último ingreso autorizado.
 * Antes esta lógica y el SQL estaban dentro del controlador REST (HU-22, SRP y DIP).
 */
public class LatestEntryService implements FindLatestEntryUseCase {
    private final OperatorRepository operators;
    private final LatestEntryRepository entries;

    public LatestEntryService(OperatorRepository operators, LatestEntryRepository entries) {
        this.operators = operators;
        this.entries = entries;
    }

    @Override
    public Optional<String> findLatestPlate(String operatorUsername) {
        var operator = operators.findActiveOperator(operatorUsername)
                .orElseThrow(OperatorNotAvailableException::new);
        return entries.findLatestAuthorizedPlate(operator.siteId());
    }
}
