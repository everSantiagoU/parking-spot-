package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.model.Occupancy;
import com.parkingspot.domain.port.out.OccupancyRepository;
import com.parkingspot.domain.port.out.OperatorRepository;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ocupación")
public class OccupancyController {
    private static final Logger log = LoggerFactory.getLogger(OccupancyController.class);

    private final OccupancyRepository occupancy;
    private final OperatorRepository operators;

    public OccupancyController(OccupancyRepository occupancy, OperatorRepository operators) {
        this.occupancy = occupancy;
        this.operators = operators;
    }

    @GetMapping("/api/v1/occupancy")
    @Operation(summary = "Consulta la capacidad y ocupación por tipo de vehículo en la sede del operador",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ocupación actual por cada tipo configurado",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Occupancy.class)))),
                    @ApiResponse(responseCode = "401", description = "Sesión requerida", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Rol o sede inválidos", content = @Content),
                    @ApiResponse(responseCode = "503", description = "Error al consultar la ocupación", content = @Content)
            })
    public List<Occupancy> findOccupancy(Authentication authentication) {
        var operator = operators.findActiveOperator(authentication.getName())
                .orElseThrow(OperatorNotAvailableException::new);
        return occupancy.findBySite(operator.siteId());
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorResponse> dataAccessFailure(DataAccessException failure) {
        log.error("Error al consultar la ocupación", failure);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("ERROR_OCUPACION", "No se pudo consultar la ocupación."));
    }
}
