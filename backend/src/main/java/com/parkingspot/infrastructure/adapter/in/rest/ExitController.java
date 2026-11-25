package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.usecase.ValidateExitUseCase;
import com.parkingspot.domain.model.ExitDecision;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Carril de salida")
public class ExitController {
    private final ValidateExitUseCase exits;

    public ExitController(ValidateExitUseCase exits) {
        this.exits = exits;
    }

    @PostMapping("/api/v1/exits/validate")
    @Operation(summary = "Valida el ingreso y el cobro del día para autorizar la barrera simulada",
            description = "Requiere sesión de OPERADOR y token CSRF. Registra el intento sin cerrar el ingreso.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Decisión de apertura o rechazo auditada"),
                    @ApiResponse(responseCode = "400", description = "Placa ausente o inválida", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Sesión requerida", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Rol, sede o token CSRF inválido", content = @Content),
                    @ApiResponse(responseCode = "503", description = "Falló la consulta o la auditoría. Barrera cerrada.",
                            content = @Content(schema = @Schema(implementation = ExitResponse.class)))
            })
    public ResponseEntity<ExitResponse> validate(@RequestBody ExitRequest request, Authentication operator) {
        var decision = exits.validate(request.plate(), operator.getName());
        return ResponseEntity.status(decision.isSystemError() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK)
                .body(new ExitResponse(decision.authorizesOpening(), decision));
    }

    public record ExitRequest(@Schema(example = "ABC123") String plate) {
    }

    public record ExitResponse(
            @Schema(description = "Autorización explícita de apertura de la barrera simulada") boolean barrierOpen,
            @Schema(description = "Motivo de la decisión") ExitDecision code) {
    }
}
