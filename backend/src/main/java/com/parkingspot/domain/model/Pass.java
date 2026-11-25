package com.parkingspot.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Pase de una placa (HU-17). No es una tabla aparte: es el cobro pagado del día operativo,
 * que la HU-16 deja vigente hasta las 06:00 del día siguiente (validUntil, exclusivo).
 */
public record Pass(long chargeId, String plate, long siteId, LocalDate operatingDay, OffsetDateTime validUntil) {

    public PassStatus statusAt(Instant now, long currentSiteId) {
        if (siteId != currentSiteId) {
            return PassStatus.OTRA_SEDE;
        }
        return now.isBefore(validUntil.toInstant()) ? PassStatus.VIGENTE : PassStatus.VENCIDO;
    }

    /** CA-04: solo un pase vigente de la misma sede permite reingresar sin cobro. */
    public boolean allowsFreeReentry(Instant now, long currentSiteId) {
        return statusAt(now, currentSiteId) == PassStatus.VIGENTE;
    }
}
