package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.usecase.RegisterEntryUseCase;
import com.parkingspot.domain.model.EntryAttempt;
import com.parkingspot.domain.model.EntryOutcome;
import com.parkingspot.domain.model.PlateCapture;
import com.parkingspot.domain.model.ReadingSource;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.infrastructure.adapter.in.rest.ClassificationErrorHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;

/**
 * Carril de entrada. La sede y el usuario salen del operador autenticado.
 * HU-11: valida la confianza y permite la lectura manual. HU-10: registra el evento de ingreso.
 * HU-13: valida el cupo antes de abrir la barrera.
 */
@RestController
@RequestMapping("/api/v1/entries")
public class EntryController {
    private static final String AUTHORIZED_EXAMPLE = """
            {"resultado":"AUTORIZADO","motivo":null,"barreraAbierta":true,"eventoId":15,"sedeId":1,\
            "ocurridoEn":"2026-11-24T18:45:00Z","readText":"ABC123","plate":"ABC123","vehicleType":"CARRO",\
            "confidence":0.93,"source":"SIMULADOR","operador":"operador1"}""";
    private static final String MANUAL_EXAMPLE = """
            {"resultado":"AUTORIZADO","motivo":null,"barreraAbierta":true,"eventoId":16,"sedeId":1,\
            "ocurridoEn":"2026-11-24T18:46:00Z","readText":"ABC12D","plate":"ABC12D","vehicleType":"MOTO",\
            "confidence":null,"source":"MANUAL","operador":"operador1"}""";
    private static final String LOW_CONFIDENCE_EXAMPLE = """
            {"resultado":"REQUIERE_VALIDACION","motivo":"CONFIANZA_BAJA","barreraAbierta":false,"eventoId":null,\
            "sedeId":null,"ocurridoEn":null,"readText":"ABC123","plate":"ABC123","vehicleType":"CARRO",\
            "confidence":0.42,"source":"SIMULADOR","operador":"operador1"}""";
    private static final String INVALID_FORMAT_EXAMPLE = """
            {"resultado":"REQUIERE_VALIDACION","motivo":"FORMATO_INVALIDO","barreraAbierta":false,"eventoId":null,\
            "sedeId":null,"ocurridoEn":null,"readText":"AB123","plate":null,"vehicleType":null,"confidence":0.97,\
            "source":"SIMULADOR","operador":"operador1"}""";
    private static final String FULL_EXAMPLE = """
            {"resultado":"LLENO","motivo":"LLENO","barreraAbierta":false,"eventoId":17,"sedeId":1,\
            "ocurridoEn":"2026-11-24T18:47:00Z","readText":"ABC123","plate":"ABC123","vehicleType":"CARRO",\
            "confidence":0.93,"source":"SIMULADOR","operador":"operador1"}""";
    private static final String FULL_DESCRIPTION = "LLENO: no hay cupo para ese tipo de vehículo en la sede. La "
            + "barrera sigue cerrada y el intento queda guardado como RECHAZADO (cuerpo EntryResponse). También 409 "
            + "INGRESO_ACTIVO si la placa ya tiene ingreso en esta sede (EntryResponse), u " +
            "OCCUPANCY_NOT_CONFIGURED si la sede no tiene cupos para ese tipo (cuerpo ErrorResponse).";
    private static final String ACCESS_DENIED = """
            {"code":"ACCESS_DENIED","message":"Acceso denegado."}""";
    private static final String OPERATOR_NOT_AVAILABLE = """
            {"code":"OPERATOR_NOT_AVAILABLE","message":"El operador no está activo o no tiene una sede asignada."}""";
    private static final String OCCUPANCY_NOT_CONFIGURED = """
            {"code":"OCCUPANCY_NOT_CONFIGURED","message":"La sede 1 no tiene cupos configurados para CARRO."}""";
    private static final String INVALID_REQUEST = """
            {"code":"INVALID_REQUEST","message":"La solicitud debe contener un JSON válido."}""";

    private final RegisterEntryUseCase registerEntry;

    public EntryController(RegisterEntryUseCase registerEntry) {
        this.registerEntry = registerEntry;
    }

    @PostMapping("/automatic")
    @Operation(
            summary = "Registra el ingreso a partir de la lectura automática",
            description = "El lector (hoy, el simulador de la HU-08) entrega la placa y la confianza. Si el formato es "
                    + "inválido o la confianza es menor que el umbral configurado, responde 200 REQUIERE_VALIDACION "
                    + "y no registra nada: el operador debe usar /api/v1/entries/manual. Si no, valida el cupo del tipo "
                    + "de vehículo en la sede: con cupo guarda el evento con la hora del servidor, ocupa el cupo y "
                    + "responde 201 AUTORIZADO; sin cupo responde 409 LLENO y la barrera sigue cerrada.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Ingreso AUTORIZADO y registrado; la barrera se abre",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EntryResponse.class),
                                    examples = @ExampleObject(name = "AUTORIZADO", value = AUTHORIZED_EXAMPLE))),
                    @ApiResponse(responseCode = "200", description = "REQUIERE_VALIDACION: no se registra la entrada "
                            + "y la barrera sigue cerrada",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EntryResponse.class), examples = {
                                    @ExampleObject(name = "CONFIANZA_BAJA", value = LOW_CONFIDENCE_EXAMPLE),
                                    @ExampleObject(name = "FORMATO_INVALIDO", value = INVALID_FORMAT_EXAMPLE)
                            })),
                    @ApiResponse(responseCode = "400", description = "Confianza fuera de [0, 1] o ausente, o JSON mal formado",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "INVALID_CONFIDENCE", value = """
                                            {"code":"INVALID_CONFIDENCE","message":"La confianza debe ser un número entre 0 y 1."}"""),
                                    @ExampleObject(name = "INVALID_REQUEST", value = INVALID_REQUEST)
                            })),
                    @ApiResponse(responseCode = "401", description = "Sin sesión, o sesión inválida o expirada",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Sin rol OPERADOR, sin token CSRF, "
                            + "u operador inactivo o sin sede",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "ACCESS_DENIED", value = ACCESS_DENIED),
                                    @ExampleObject(name = "OPERATOR_NOT_AVAILABLE", value = OPERATOR_NOT_AVAILABLE)
                            })),
                    @ApiResponse(responseCode = "409", description = FULL_DESCRIPTION,
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(oneOf = {EntryResponse.class, ErrorResponse.class}), examples = {
                                    @ExampleObject(name = "LLENO", value = FULL_EXAMPLE),
                                    @ExampleObject(name = "OCCUPANCY_NOT_CONFIGURED", value = OCCUPANCY_NOT_CONFIGURED)
                            }))
            })
    public ResponseEntity<EntryResponse> automatic(@RequestBody AutomaticEntryRequest request, Principal principal) {
        return respond(registerEntry.enterAutomatic(
                new PlateCapture(request.plate(), request.confidence()), principal.getName()));
    }

    @PostMapping("/manual")
    @Operation(
            summary = "Registra el ingreso con la placa digitada o confirmada por el operador",
            description = "El operador digita o confirma la placa que ve en el carril (por ejemplo, después de un "
                    + "REQUIERE_VALIDACION). El evento queda con origen MANUAL, sin confianza y con el usuario del "
                    + "operador autenticado. También valida el cupo: sin cupo responde 409 LLENO.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Ingreso AUTORIZADO y registrado; la barrera se abre",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EntryResponse.class),
                                    examples = @ExampleObject(name = "MANUAL", value = MANUAL_EXAMPLE))),
                    @ApiResponse(responseCode = "400", description = "Placa con formato inválido o JSON mal formado",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "INVALID_PLATE", value = """
                                            {"code":"INVALID_PLATE","message":"La placa debe tener el formato ABC123 (carro) o ABC12D (moto)."}"""),
                                    @ExampleObject(name = "INVALID_REQUEST", value = INVALID_REQUEST)
                            })),
                    @ApiResponse(responseCode = "401", description = "Sin sesión, o sesión inválida o expirada",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Sin rol OPERADOR, sin token CSRF, "
                            + "u operador inactivo o sin sede",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "ACCESS_DENIED", value = ACCESS_DENIED),
                                    @ExampleObject(name = "OPERATOR_NOT_AVAILABLE", value = OPERATOR_NOT_AVAILABLE)
                            })),
                    @ApiResponse(responseCode = "409", description = FULL_DESCRIPTION,
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(oneOf = {EntryResponse.class, ErrorResponse.class}), examples = {
                                    @ExampleObject(name = "LLENO", value = FULL_EXAMPLE),
                                    @ExampleObject(name = "OCCUPANCY_NOT_CONFIGURED", value = OCCUPANCY_NOT_CONFIGURED)
                            }))
            })
    public ResponseEntity<EntryResponse> manual(@RequestBody ManualEntryRequest request, Principal principal) {
        return respond(registerEntry.enterManual(request.plate(), principal.getName()));
    }

    private static ResponseEntity<EntryResponse> respond(EntryAttempt attempt) {
        var status = switch (attempt.outcome()) {
            case AUTORIZADO -> HttpStatus.CREATED;
            case REQUIERE_VALIDACION -> HttpStatus.OK;
            case LLENO, INGRESO_ACTIVO -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(EntryResponse.from(attempt));
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

    @Schema(description = "Resultado del intento de ingreso en el carril")
    public record EntryResponse(
            @Schema(example = "AUTORIZADO") EntryOutcome resultado,
            @Schema(description = "Motivo cuando no se autoriza", example = "CONFIANZA_BAJA") String motivo,
            @Schema(description = "true solo si la entrada se autorizó", example = "true") boolean barreraAbierta,
            @Schema(description = "Evento guardado; null si no se registró", example = "15") Long eventoId,
            @Schema(description = "Sede del operador; null si no se registró", example = "1") Long sedeId,
            @Schema(description = "Hora del servidor del evento; null si no se registró",
                    example = "2026-11-24T18:45:00Z") Instant ocurridoEn,
            @Schema(description = "Texto tal como lo entregó el lector o el operador", example = "ABC123") String readText,
            @Schema(description = "Placa normalizada; null si el formato es inválido", example = "ABC123") String plate,
            @Schema(example = "CARRO") VehicleType vehicleType,
            @Schema(description = "Confianza de 0 a 1; null en una lectura manual", example = "0.93") Double confidence,
            @Schema(example = "SIMULADOR") ReadingSource source,
            @Schema(description = "Usuario del operador autenticado", example = "operador1") String operador) {

        static EntryResponse from(EntryAttempt attempt) {
            return new EntryResponse(attempt.outcome(), attempt.reason(), attempt.barrierOpen(), attempt.eventId(),
                    attempt.siteId(), attempt.occurredAt(), attempt.readText(), attempt.plate(), attempt.type(),
                    attempt.confidence() == null ? null : attempt.confidence().value(), attempt.source(),
                    attempt.operatorUsername());
        }
    }
}
