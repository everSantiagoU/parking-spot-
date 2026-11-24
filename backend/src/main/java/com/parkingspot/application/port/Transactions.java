package com.parkingspot.application.port;

import java.util.function.Supplier;

/** Ejecuta un bloque de trabajo dentro de una transacción de base de datos. */
public interface Transactions {
    <T> T inTransaction(Supplier<T> work);
}
