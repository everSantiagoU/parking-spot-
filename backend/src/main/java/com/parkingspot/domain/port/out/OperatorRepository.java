package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.Operator;

import java.util.Optional;

/** Consulta el operador autenticado y su sede. */
public interface OperatorRepository {
    /** Operador activo con sede asignada, o vacío si no existe, está inactivo o no tiene sede. */
    Optional<Operator> findActiveOperator(String username);
}
