package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.port.out.OperatorRepository;
import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public record LatestEntryController(JdbcClient jdbc, OperatorRepository operators) {
    public record LatestEntry(String plate) {}

    @GetMapping("/api/v1/entries/latest")
    @Operation(summary = "Última placa con ingreso autorizado en la sede del operador")
    public LatestEntry latest(Principal principal) {
        long site = operators.findActiveOperator(principal.getName()).orElseThrow(OperatorNotAvailableException::new).siteId();
        return new LatestEntry(jdbc.sql("""
                SELECT placa FROM eventos WHERE sede_id = :site AND tipo_evento = 'INGRESO' AND resultado = 'AUTORIZADO'
                ORDER BY ocurrido_en DESC, id DESC LIMIT 1
                """).param("site", site).query(String.class).optional().orElse(null));
    }
}
