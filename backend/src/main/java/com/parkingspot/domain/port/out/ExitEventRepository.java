package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.ExitDecision;
import com.parkingspot.domain.model.Operator;
import com.parkingspot.domain.model.VehicleClassification;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface ExitEventRepository {
    Optional<ActiveEntry> findActiveEntry(long siteId, String plate);

    void recordAttempt(Operator operator, VehicleClassification vehicle, Instant now, ExitDecision decision,
                       Long chargeId);

    record ActiveEntry(long id, LocalDate exemptOperatingDay) {
        public boolean isExemptOn(LocalDate day) {
            return day.equals(exemptOperatingDay);
        }
    }
}
