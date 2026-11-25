package com.parkingspot.application.service;

import com.parkingspot.application.dto.PassResult;
import com.parkingspot.application.usecase.CheckFreeReentryUseCase;
import com.parkingspot.application.usecase.FindPassUseCase;
import com.parkingspot.domain.exception.PassNotFoundException;
import com.parkingspot.domain.model.Pass;
import com.parkingspot.domain.model.VehicleClassification;
import com.parkingspot.domain.port.out.PassRepository;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.Clock;
import java.time.Instant;

/** Consulta y vigencia de pases (HU-17). */
public class PassService implements FindPassUseCase, CheckFreeReentryUseCase {
    private final PlateClassifier plateClassifier;
    private final PassRepository passes;
    private final Clock clock;

    public PassService(PlateClassifier plateClassifier, PassRepository passes, Clock clock) {
        this.plateClassifier = plateClassifier;
        this.passes = passes;
        this.clock = clock;
    }

    @Override
    public PassResult find(String plate, long siteId) {
        VehicleClassification vehicle = plateClassifier.classify(plate);
        Pass pass = passes.findLatest(vehicle.plate(), siteId)
                .orElseThrow(() -> new PassNotFoundException(vehicle.plate()));
        return new PassResult(pass, pass.statusAt(clock.instant(), siteId));
    }

    @Override
    public boolean hasValidPass(String plate, long siteId) {
        VehicleClassification vehicle = plateClassifier.classify(plate);
        Instant now = clock.instant();
        return passes.findLatest(vehicle.plate(), siteId)
                .map(pass -> pass.allowsFreeReentry(now, siteId))
                .orElse(false);
    }
}
