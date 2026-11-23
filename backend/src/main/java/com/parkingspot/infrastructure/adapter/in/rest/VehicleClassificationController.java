package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.usecase.ClassifyVehicleUseCase;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.model.VehicleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleClassificationController {
    private final ClassifyVehicleUseCase classifyVehicle;

    public VehicleClassificationController(ClassifyVehicleUseCase classifyVehicle) {
        this.classifyVehicle = classifyVehicle;
    }

    @PostMapping("/classify")
    @Operation(summary = "Clasifica una placa colombiana", responses = {
            @ApiResponse(responseCode = "200", description = "Placa normalizada y tipo de vehículo"),
            @ApiResponse(responseCode = "400", description = "Placa ausente o con formato inválido", content = @Content)
    })
    public ClassificationResponse classify(@RequestBody ClassificationRequest request) {
        if (request == null) {
            throw new InvalidPlateException();
        }
        var result = classifyVehicle.classify(request.plate());
        return new ClassificationResponse(result.plate(), result.type());
    }

    public record ClassificationRequest(String plate) {
    }

    public record ClassificationResponse(String plate, VehicleType type) {
    }
}
