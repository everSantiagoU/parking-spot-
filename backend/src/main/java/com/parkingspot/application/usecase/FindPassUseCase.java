package com.parkingspot.application.usecase;

import com.parkingspot.application.dto.PassResult;

/** Consulta el pase de una placa y su estado en la sede del operador (HU-17). */
public interface FindPassUseCase {
    PassResult find(String plate, long siteId);
}
