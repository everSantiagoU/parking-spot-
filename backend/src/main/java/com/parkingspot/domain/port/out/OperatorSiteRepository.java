package com.parkingspot.domain.port.out;

import java.util.Optional;

/** Sede a la que pertenece un usuario operador. */
public interface OperatorSiteRepository {
    Optional<Long> findSiteOf(String username);
}
