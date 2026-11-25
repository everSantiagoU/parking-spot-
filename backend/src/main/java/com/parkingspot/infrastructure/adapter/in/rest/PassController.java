package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.dto.PassResult;
import com.parkingspot.application.usecase.FindPassUseCase;
import com.parkingspot.domain.model.Pass;
import com.parkingspot.domain.model.PassStatus;
import com.parkingspot.domain.port.out.OperatorSiteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passes")
@Tag(name = "Pases", description = "Consulta del pase vigente de una placa (HU-17)")
public class PassController {
    private final FindPassUseCase findPass;
    private final OperatorSiteRepository operatorSites;

    public PassController(FindPassUseCase findPass, OperatorSiteRepository operatorSites) {
        this.findPass = findPass;
        this.operatorSites = operatorSites;
    }

    @GetMapping
    @Operation(summary = "Consulta el pase de una placa y si permite reingresar sin cobro en la sede del operador",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pase encontrado con estado VIGENTE, VENCIDO u OTRA_SEDE"),
                    @ApiResponse(responseCode = "400", description = "Placa con formato inválido", content = @Content),
                    @ApiResponse(responseCode = "404", description = "La placa no tiene pase", content = @Content),
                    @ApiResponse(responseCode = "409", description = "El operador no tiene sede asignada", content = @Content)
            })
    public PassResponse find(@RequestParam String plate, Authentication operator) {
        long siteId = operatorSites.findSiteOf(operator.getName())
                .orElseThrow(ChargeController.OperatorWithoutSiteException::new);
        return PassResponse.from(findPass.find(plate, siteId));
    }

    public record PassResponse(
            String plate,
            long chargeId,
            long siteId,
            LocalDate operatingDay,
            OffsetDateTime validUntil,
            PassStatus status,
            boolean freeReentry) {

        static PassResponse from(PassResult result) {
            Pass pass = result.pass();
            return new PassResponse(pass.plate(), pass.chargeId(), pass.siteId(), pass.operatingDay(),
                    pass.validUntil(), result.status(), result.allowsFreeReentry());
        }
    }
}
