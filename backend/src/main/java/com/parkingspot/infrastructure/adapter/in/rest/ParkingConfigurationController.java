package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.service.ParkingConfigurationService;
import com.parkingspot.application.service.ParkingConfigurationService.Configuration;
import com.parkingspot.application.service.ParkingConfigurationService.Site;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/sites")
public record ParkingConfigurationController(ParkingConfigurationService configuration) {
    @GetMapping
    public List<Site> sites() { return configuration.sites(); }

    @GetMapping("/{site}/configuration")
    public Configuration read(@PathVariable long site) { return configuration.read(site); }

    @PutMapping("/{site}/configuration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable long site, @RequestBody Configuration data) { configuration.save(site, data); }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ClassificationErrorHandler.ErrorResponse> invalid(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(
                new ClassificationErrorHandler.ErrorResponse("CONFIGURACION_INVALIDA", ex.getReason()));
    }
}
