package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.domain.exception.InvalidParkingConfigurationException;
import com.parkingspot.domain.exception.SiteNotAvailableException;
import com.parkingspot.infrastructure.adapter.out.persistence.ParkingConfigurationStore;
import com.parkingspot.infrastructure.adapter.out.persistence.ParkingConfigurationStore.Configuration;
import com.parkingspot.infrastructure.adapter.out.persistence.ParkingConfigurationStore.Site;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sites")
public record ParkingConfigurationController(ParkingConfigurationStore configuration) {
    @GetMapping
    public List<Site> sites() { return configuration.sites(); }

    @GetMapping("/{site}/configuration")
    public Configuration read(@PathVariable long site) { return configuration.read(site); }

    @PutMapping("/{site}/configuration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable long site, @RequestBody Configuration data) { configuration.save(site, data); }

    /** La persistencia lanza excepciones del dominio; aquí se traducen a HTTP (HU-22, SRP). */
    @ExceptionHandler(InvalidParkingConfigurationException.class)
    ResponseEntity<ClassificationErrorHandler.ErrorResponse> invalid(InvalidParkingConfigurationException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(SiteNotAvailableException.class)
    ResponseEntity<ClassificationErrorHandler.ErrorResponse> siteNotAvailable(SiteNotAvailableException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private static ResponseEntity<ClassificationErrorHandler.ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                new ClassificationErrorHandler.ErrorResponse("CONFIGURACION_INVALIDA", message));
    }
}
