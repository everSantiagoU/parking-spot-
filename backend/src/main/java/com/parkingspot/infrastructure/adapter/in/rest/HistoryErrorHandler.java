package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.InvalidHistoryFilterException;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Errores de validación de los filtros del historial (HU-20 CP-X01). */
@RestControllerAdvice(assignableTypes = HistoryController.class)
public class HistoryErrorHandler {

    @ExceptionHandler(InvalidHistoryFilterException.class)
    ResponseEntity<ErrorResponse> invalidFilter(InvalidHistoryFilterException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("FILTRO_INVALIDO", exception.getMessage()));
    }

    /** Una fecha mal escrita o un estado/tipo que no existe. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> invalidParameter(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("FILTRO_INVALIDO", "El valor del filtro " + exception.getName() + " no es válido."));
    }
}
