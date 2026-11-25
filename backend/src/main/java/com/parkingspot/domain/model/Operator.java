package com.parkingspot.domain.model;

import java.util.Objects;

/**
 * Operador autenticado que trabaja en un carril. La sede sale de su cuenta (usuarios.sede_id),
 * nunca del cuerpo de la petición.
 */
public record Operator(long id, String username, long siteId) {
    public Operator {
        Objects.requireNonNull(username, "username");
    }
}
