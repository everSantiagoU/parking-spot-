package com.parkingspot.application.usecase;

import java.util.Optional;

/** Caso de uso: última placa autorizada en la sede del operador autenticado. */
public interface FindLatestEntryUseCase {
    Optional<String> findLatestPlate(String operatorUsername);
}
