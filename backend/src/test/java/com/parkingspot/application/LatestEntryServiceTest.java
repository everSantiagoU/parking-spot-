package com.parkingspot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.application.service.LatestEntryService;
import com.parkingspot.domain.exception.OperatorNotAvailableException;
import com.parkingspot.domain.model.Operator;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** HU-22: el caso de uso depende de abstracciones (DIP), así que se prueba sin base de datos ni Spring. */
class LatestEntryServiceTest {

    @Test
    void devuelveLaUltimaPlacaDeLaSedeDelOperador() {
        var service = new LatestEntryService(
                username -> Optional.of(new Operator(1L, username, 7L)),
                siteId -> siteId == 7L ? Optional.of("ABC123") : Optional.empty());

        assertThat(service.findLatestPlate("operador")).contains("ABC123");
    }

    @Test
    void sinIngresosDevuelveVacio() {
        var service = new LatestEntryService(
                username -> Optional.of(new Operator(1L, username, 7L)),
                siteId -> Optional.empty());

        assertThat(service.findLatestPlate("operador")).isEmpty();
    }

    @Test
    void operadorSinSedeEsRechazado() {
        var service = new LatestEntryService(username -> Optional.empty(), siteId -> Optional.of("ABC123"));

        assertThatThrownBy(() -> service.findLatestPlate("operador"))
                .isInstanceOf(OperatorNotAvailableException.class);
    }
}
