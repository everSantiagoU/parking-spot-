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
    @Operation(summary = "Registra una salida autorizada y libera el cupo del ingreso",
            description = "Requiere sesión de OPERADOR y token CSRF. La apertura confirma la salida efectiva y la liberación del cupo.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Salida confirmada o intento de salida rechazado"),
                    @ApiResponse(responseCode = "400", description = "Placa ausente o inválida", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Sesión requerida", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Rol, sede o token CSRF inválido", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Ocupación inconsistente. Salida revertida.",
                            content = @Content(schema = @Schema(implementation = ExitResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Falló la consulta, el registro o la auditoría.",
                            content = @Content(schema = @Schema(implementation = ExitResponse.class)))
            })
    public ResponseEntity<ExitResponse> validate(@RequestBody ExitRequest request, Authentication operator) {
        var decision = exits.validate(request.plate(), operator.getName());
        var status = switch (decision) {
            case OCUPACION_INCONSISTENTE -> HttpStatus.CONFLICT;
            case ERROR_CONSULTA, ERROR_REGISTRO_SALIDA, ERROR_AUDITORIA -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(new ExitResponse(decision.authorizesOpening(), decision));
    }

    public record ExitRequest(@Schema(example = "ABC123") String plate) {
    }

    public record ExitResponse(
            @Schema(description = "Autorización explícita de apertura de la barrera simulada") boolean barrierOpen,
            @Schema(description = "Motivo de la decisión") ExitDecision code) {
    }
}
