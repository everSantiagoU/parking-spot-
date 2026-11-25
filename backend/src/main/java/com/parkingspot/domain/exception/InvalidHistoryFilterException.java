package com.parkingspot.domain.exception;

/** Los filtros del historial no son válidos (por ejemplo, un intervalo de fechas invertido). */
public class InvalidHistoryFilterException extends RuntimeException {
    public InvalidHistoryFilterException(String message) {
        super(message);
    }
}
