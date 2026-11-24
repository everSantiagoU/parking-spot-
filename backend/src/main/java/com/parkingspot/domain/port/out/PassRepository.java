package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Pass;
import java.util.Optional;

/** Consulta de pases (cobros pagados) de una placa. */
public interface PassRepository {

    /**
     * Pase más reciente de la placa. Si la placa tiene pase en la sede indicada se prefiere ese,
     * así un pase de otra sede no oculta el de la sede del operador.
     */
    Optional<Pass> findLatest(String plate, long preferredSiteId);
}
