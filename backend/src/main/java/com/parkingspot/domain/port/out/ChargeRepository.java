package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Charge;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Patrón Repository: persistencia de cobros vista como una colección del dominio.
 * Los casos de uso dependen de esta interfaz y no saben que detrás hay PostgreSQL
 * ({@code JdbcChargeRepository}); en las pruebas se reemplaza por una versión en memoria.
 */
public interface ChargeRepository {

    Optional<Charge> findByPlateAndSiteAndDay(String plate, long siteId, LocalDate operatingDay);

    /**
     * Guarda el cobro solo si no existe otro para la misma placa, sede y día operativo.
     * Es atómico: con dos solicitudes simultáneas queda un único cobro.
     */
    SavedCharge saveIfAbsent(Charge charge);

    /** Cobro que quedó guardado y si lo creó esta llamada o ya existía. */
    record SavedCharge(Charge charge, boolean created) {
    }
}
