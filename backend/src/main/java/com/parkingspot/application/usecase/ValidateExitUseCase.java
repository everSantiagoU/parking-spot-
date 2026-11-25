package com.parkingspot.application.usecase;

import com.parkingspot.domain.model.ExitDecision;

public interface ValidateExitUseCase {
    ExitDecision validate(String plate, String operatorUsername);
}
