package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.usecase.FindLatestEntryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solo traduce HTTP: la consulta vive en el caso de uso y en su repositorio (HU-22, SRP). */
@RestController
public record LatestEntryController(FindLatestEntryUseCase latestEntries) {
    public record LatestEntry(String plate) {}

    @GetMapping("/api/v1/entries/latest")
    @Operation(summary = "Última placa con ingreso autorizado en la sede del operador")
    public LatestEntry latest(Principal principal) {
        return new LatestEntry(latestEntries.findLatestPlate(principal.getName()).orElse(null));
    }
}
