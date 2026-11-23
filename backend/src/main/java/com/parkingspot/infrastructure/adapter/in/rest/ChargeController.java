package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.dto.ChargeResult;
import com.parkingspot.application.usecase.FindTodayChargeUseCase;
import com.parkingspot.application.usecase.GenerateChargeUseCase;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.OperatorSiteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/charges")
@Tag(name = "Cobros", description = "Cálculo del cobro del día operativo (HU-14 y HU-15)")
public class ChargeController {
    private final GenerateChargeUseCase generateCharge;
    private final FindTodayChargeUseCase findTodayCharge;
    private final OperatorSiteRepository operatorSites;

    public ChargeController(GenerateChargeUseCase generateCharge, FindTodayChargeUseCase findTodayCharge,
                            OperatorSiteRepository operatorSites) {
        this.generateCharge = generateCharge;
        this.findTodayCharge = findTodayCharge;
        this.operatorSites = operatorSites;
    }

    @PostMapping
    @Operation(summary = "Calcula y registra el cobro del día operativo para un ingreso", responses = {
            @ApiResponse(responseCode = "201", description = "Cobro nuevo creado (outcome CREADO)"),
            @ApiResponse(responseCode = "200", description = "Reingreso con cobro existente (EXISTENTE) o vehículo sin obligación de pago (EXENTO)"),
            @ApiResponse(responseCode = "400", description = "Placa ausente o con formato inválido", content = @Content),
            @ApiResponse(responseCode = "409", description = "El operador no tiene sede asignada", content = @Content),
            @ApiResponse(responseCode = "422", description = "No hay franja o tarifa aplicable", content = @Content)
    })
    public ResponseEntity<ChargeResponse> generate(@RequestBody ChargeRequest request, Authentication operator) {
        if (request == null) {
            throw new InvalidPlateException();
        }
        ChargeResult result = generateCharge.generate(request.plate(), siteOf(operator));
        HttpStatus status = result.outcome() == ChargeResult.Outcome.CREADO ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ChargeResponse.from(result));
    }

    @GetMapping("/today")
    @Operation(summary = "Consulta el cobro de una placa en el día operativo actual", responses = {
            @ApiResponse(responseCode = "200", description = "Cobro encontrado"),
            @ApiResponse(responseCode = "400", description = "Placa con formato inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "La placa no tiene cobro hoy", content = @Content)
    })
    public ChargeResponse today(@RequestParam String plate, Authentication operator) {
        Charge charge = findTodayCharge.findToday(plate, siteOf(operator));
        return ChargeResponse.from(ChargeResult.existing(charge));
    }

    private long siteOf(Authentication operator) {
        return operatorSites.findSiteOf(operator.getName()).orElseThrow(OperatorWithoutSiteException::new);
    }

    public record ChargeRequest(String plate) {
    }

    public record ChargeResponse(
            String plate,
            VehicleType vehicleType,
            LocalDate operatingDay,
            TimeSlot timeSlot,
            ChargeResult.Outcome outcome,
            Long chargeId,
            BigDecimal appliedTariff,
            BigDecimal amount,
            ChargeStatus status,
            OffsetDateTime generatedAt) {

        static ChargeResponse from(ChargeResult result) {
            Charge charge = result.charge();
            if (charge == null) {
                return new ChargeResponse(result.plate(), result.vehicleType(), result.operatingDay(),
                        result.timeSlot(), result.outcome(), null, null, BigDecimal.ZERO, null, null);
            }
            return new ChargeResponse(charge.plate(), charge.vehicleType(), charge.operatingDay(),
                    charge.timeSlot(), result.outcome(), charge.id(), charge.appliedTariff(), charge.amount(),
                    charge.status(), charge.generatedAt());
        }
    }

    static class OperatorWithoutSiteException extends RuntimeException {
        OperatorWithoutSiteException() {
            super("El usuario no tiene una sede asignada.");
        }
    }
}
