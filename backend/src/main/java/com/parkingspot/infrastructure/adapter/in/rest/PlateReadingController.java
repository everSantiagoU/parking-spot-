package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.usecase.ReadPlateUseCase;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plate-readings")
public class PlateReadingController {
    private final ReadPlateUseCase readPlate;

    public PlateReadingController(ReadPlateUseCase readPlate) {
        this.readPlate = readPlate;
    }

    @PostMapping("/simulate")
    @Operation(
            summary = "Simula la lectura de una placa",
            description = "Recibe la placa y la confianza que enviaría una cámara, normaliza y clasifica la placa "
                    + "y responde con la fuente SIMULADOR. No aplica umbral de confianza ni registra el ingreso.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lectura simulada, normalizada y clasificada",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PlateReadingResponse.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Confianza fuera de [0, 1] o ausente, placa con formato inválido o JSON mal formado",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "INVALID_CONFIDENCE", value = """
                                            {"code":"INVALID_CONFIDENCE","message":"La confianza debe ser un número entre 0 y 1."}"""),
                                    @ExampleObject(name = "INVALID_PLATE", value = """
                                            {"code":"INVALID_PLATE","message":"La placa debe tener el formato ABC123 (carro) o ABC12D (moto)."}"""),
                                    @ExampleObject(name = "INVALID_REQUEST", value = """
                                            {"code":"INVALID_REQUEST","message":"La solicitud debe contener un JSON válido."}""")
                            })),
                    @ApiResponse(responseCode = "401", description = "Sin sesión, o sesión inválida o expirada",
                            content = @Content),
                    @ApiResponse(responseCode = "403",
                            description = "La sesión no tiene el rol OPERADOR o falta el token CSRF",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples =
                                    @ExampleObject(name = "ACCESS_DENIED", value = """
                                            {"code":"ACCESS_DENIED","message":"Acceso denegado."}""")))
            })
    public PlateReadingResponse simulate(@RequestBody SimulatedReadingRequest request) {
        var reading = readPlate.read(new PlateCapture(request.plate(), request.confidence()));
        return new PlateReadingResponse(reading.plate(), reading.type(), reading.confidence().value(), reading.source());
    }

    @Schema(description = "Lectura que enviaría la cámara")
    public record SimulatedReadingRequest(
            @Schema(description = "Placa leída; se normaliza (espacios y mayúsculas)", example = " abc12d ")
            String plate,
            @Schema(description = "Confianza de la lectura, entre 0 y 1 (ambos incluidos)", example = "0.93",
                    minimum = "0", maximum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Double confidence) {
    }

    @Schema(description = "Lectura simulada ya normalizada y clasificada")
    public record PlateReadingResponse(
            @Schema(example = "ABC12D") String plate,
            @Schema(example = "MOTO") VehicleType vehicleType,
            @Schema(example = "0.93") double confidence,
            @Schema(example = "SIMULADOR") ReadingSource source) {
    }
}
