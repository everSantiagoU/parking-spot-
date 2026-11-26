package com.parkingspot.domain.port.out;

import java.util.Optional;

/** Consulta la última placa con ingreso autorizado en una sede. */
public interface LatestEntryRepository {
    Optional<String> findLatestAuthorizedPlate(long siteId);
}
