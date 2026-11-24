package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.usecase.ValidateEntryReadingUseCase;
import com.parkingspot.domain.model.AcceptedReading;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.ReadingEvaluation;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.ValidationReason;
import com.parkingspot.domain.model.ValidationStatus;
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

import java.security.Principal;

/**
 * Flujo de ingreso de un vehículo. La sede y el usuario salen del operador autenticado.
 * HU-11: valida la confianza y permite la confirmación manual de la placa.
 */
@RestController
@RequestMapping("/api/v1/entries")
public class EntryController {
    private final ValidateEntryReadingUseCase validateReading;

    public EntryController(ValidateEntryReadingUseCase validateReading) {
        this.validateReading = validateReading;
    }

    @PostMapping("/automatic")
    @Operation(
            summary = "Valida la lectura automática de una placa",
            description = "El lector (hoy, el simulador de la HU-08) entrega la placa y la confianza. Si el formato es "
                    + "inválido o la confianza es menor que el umbral configurado, responde REQUIERE_VALIDACION y no "
                    + "registra la entrada: el operador debe usar /api/v1/entries/manual. Si no, responde ACEPTADA.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ACEPTADA o REQUIERE_VALIDACION",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EntryResponse.class), examples = {
                                    @ExampleObject(name = "ACEPTADA", value = """
                                            {"resultado":"ACEPTADA","motivo":null,"readText":"ABC123","plate":"ABC123",\
                                            "vehicleType":"CARRO","confidence":0.93,"source":"SIMULADOR","operador":"operador1"}"""),
                                    @ExampleObject(name = "REQUIERE_VALIDACION", value = """
                                            {"resultado":"REQUIERE_VALIDACION","motivo":"CONFIANZA_BAJA","readText":"ABC123",\
                                            "plate":"ABC123","vehicleType":"CARRO","confidence":0.42,"source":"SIMULADOR",\
                                            "operador":"operador1"}"""),
                                    @ExampleObject(name = "FORMATO_INVALIDO", value = """
                                            {"resultado":"REQUIERE_VALIDACION","motivo":"FORMATO_INVALIDO","readText":"AB123",\
                                            "plate":null,"vehicleType":null,"confidence":0.97,"source":"SIMULADOR",\
                                            "operador":"operador1"}""")
                            })),
                    @ApiResponse(responseCode = "400", description = "Confianza fuera de [0, 1] o ausente, o JSON mal formado",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "INVALID_CONFIDENCE", value = """
                                            {"code":"INVALID_CONFIDENCE","message":"La confianza debe ser un número entre 0 y 1."}"""),
                                    @ExampleObject(name = "INVALID_REQUEST", value = """
                                            {"code":"INVALID_REQUEST","message":"La solicitud debe contener un JSON válido."}""")
                            })),
                    @ApiResponse(responseCode = "401", description = "Sin sesión, o sesión inválida o expirada",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Sin rol OPERADOR, sin token CSRF, "
                            + "u operador inactivo o sin sede",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "ACCESS_DENIED", value = """
                                            {"code":"ACCESS_DENIED","message":"Acceso denegado."}"""),
                                    @ExampleObject(name = "OPERATOR_NOT_AVAILABLE", value = """
                                            {"code":"OPERATOR_NOT_AVAILABLE","message":"El operador no está activo o no tiene una sede asignada."}""")
                            }))
            })
    public EntryResponse automatic(@RequestBody AutomaticEntryRequest request, Principal principal) {
        var evaluation = validateReading.validateAutomatic(
                new PlateCapture(request.plate(), request.confidence()), principal.getName());
        return EntryResponse.from(evaluation, principal.getName());
    }

    @PostMapping("/manual")
    @Operation(
            summary = "Confirma o digita la placa manualmente",
            description = "El operador digita o confirma la placa que ve en el carril. La lectura queda con origen "
                    + "MANUAL, sin confianza y asociada al operador autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lectura manual aceptada",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EntryResponse.class), examples =
                                    @ExampleObject(name = "MANUAL", value = """
                                            {"resultado":"ACEPTADA","motivo":null,"readText":"ABC12D","plate":"ABC12D",\
                                            "vehicleType":"MOTO","confidence":null,"source":"MANUAL","operador":"operador1"}"""))),
                    @ApiResponse(responseCode = "400", description = "Placa con formato inválido o JSON mal formado",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "INVALID_PLATE", value = """
                                            {"code":"INVALID_PLATE","message":"La placa debe tener el formato ABC123 (carro) o ABC12D (moto)."}"""),
                                    @ExampleObject(name = "INVALID_REQUEST", value = """
                                            {"code":"INVALID_REQUEST","message":"La solicitud debe contener un JSON válido."}""")
                            })),
                    @ApiResponse(responseCode = "401", description = "Sin sesión, o sesión inválida o expirada",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Sin rol OPERADOR, sin token CSRF, "
                            + "u operador inactivo o sin sede",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "ACCESS_DENIED", value = """
                                            {"code":"ACCESS_DENIED","message":"Acceso denegado."}"""),
                                    @ExampleObject(name = "OPERATOR_NOT_AVAILABLE", value = """
                                            {"code":"OPERATOR_NOT_AVAILABLE","message":"El operador no está activo o no tiene una sede asignada."}""")
                            }))
            })
    public EntryResponse manual(@RequestBody ManualEntryRequest request, Principal principal) {
        var accepted = validateReading.confirmManual(request.plate(), principal.getName());
        return EntryResponse.from(accepted, request.plate());
    }

    @Schema(description = "Lectura que entrega el lector de placas")
    public record AutomaticEntryRequest(
            @Schema(description = "Placa leída; se normaliza (espacios y mayúsculas)", example = "ABC123")
            String plate,
            @Schema(description = "Confianza de la lectura, entre 0 y 1", example = "0.93", minimum = "0",
                    maximum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Double confidence) {
    }

    @Schema(description = "Placa digitada o confirmada por el operador")
    public record ManualEntryRequest(
            @Schema(description = "Placa que el operador ve en el carril", example = "ABC12D",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String plate) {
    }

    @Schema(description = "Resultado de la lectura en el carril de entrada")
    public record EntryResponse(
            @Schema(example = "ACEPTADA") ValidationStatus resultado,
            @Schema(description = "Motivo cuando requiere validación", example = "CONFIANZA_BAJA") ValidationReason motivo,
            @Schema(description = "Texto tal como lo entregó el lector o el operador", example = "ABC123") String readText,
            @Schema(description = "Placa normalizada; null si el formato es inválido", example = "ABC123") String plate,
            @Schema(example = "CARRO") VehicleType vehicleType,
            @Schema(description = "Confianza de 0 a 1; null en una lectura manual", example = "0.93") Double confidence,
            @Schema(example = "SIMULADOR") ReadingSource source,
            @Schema(description = "Usuario del operador autenticado", example = "operador1") String operador) {

        static EntryResponse from(ReadingEvaluation evaluation, String operator) {
            return new EntryResponse(evaluation.status(), evaluation.reason(), evaluation.readText(),
                    evaluation.plate(), evaluation.type(), evaluation.confidence().value(), evaluation.source(),
                    operator);
        }

        static EntryResponse from(AcceptedReading accepted, String typedText) {
            return new EntryResponse(ValidationStatus.ACEPTADA, null, typedText, accepted.plate(), accepted.type(),
                    accepted.confidence() == null ? null : accepted.confidence().value(), accepted.source(),
                    accepted.operator().username());
        }
    }
}
