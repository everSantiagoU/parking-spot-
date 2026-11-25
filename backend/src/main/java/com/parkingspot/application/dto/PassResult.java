package com.parkingspot.application.dto;

import com.parkingspot.domain.model.Pass;
import com.parkingspot.domain.model.PassStatus;

/** Pase consultado junto con su estado al momento de la consulta. */
public record PassResult(Pass pass, PassStatus status) {

    public boolean allowsFreeReentry() {
        return status == PassStatus.VIGENTE;
    }
}
