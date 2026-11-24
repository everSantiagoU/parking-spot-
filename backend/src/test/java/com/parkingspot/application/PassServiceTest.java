package com.parkingspot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.application.dto.PassResult;
import com.parkingspot.application.service.PassService;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.exception.PassNotFoundException;
import com.parkingspot.domain.model.Pass;
import com.parkingspot.domain.model.PassStatus;
import com.parkingspot.domain.port.out.PassRepository;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias de la consulta de pases (HU-17). Reloj: 23/11/2026 a las 20:00 de Bogotá. */
class PassServiceTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final long SEDE = 1L;
    private static final long OTRA_SEDE = 2L;

    private final List<Pass> stored = new ArrayList<>();
    private final Clock clock = Clock.fixed(LocalDateTime.of(2026, 11, 23, 20, 0).atZone(BOGOTA).toInstant(), BOGOTA);
    private final PassService service = new PassService(new PlateClassifier(), new InMemoryPasses(), clock);

    @Test
    @DisplayName("CA-01 / CA-02: la placa con cobro pagado hoy tiene pase vigente")
    void paseVigente() {
        stored.add(pass(1L, "ABC123", SEDE, LocalDate.of(2026, 11, 23)));

        PassResult result = service.find("ABC123", SEDE);

        assertThat(result.status()).isEqualTo(PassStatus.VIGENTE);
        assertThat(result.allowsFreeReentry()).isTrue();
        assertThat(service.hasValidPass("ABC123", SEDE)).isTrue();
    }

    @Test
    @DisplayName("La placa se normaliza antes de buscar el pase")
    void normalizaPlaca() {
        stored.add(pass(1L, "ABC123", SEDE, LocalDate.of(2026, 11, 23)));

        assertThat(service.find("  abc123 ", SEDE).pass().plate()).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("CP-HU-17-X01 / CA-03: el pase de un día operativo anterior está vencido")
    void paseVencido() {
        stored.add(pass(1L, "ABC123", SEDE, LocalDate.of(2026, 11, 22)));

        assertThat(service.find("ABC123", SEDE).status()).isEqualTo(PassStatus.VENCIDO);
        assertThat(service.hasValidPass("ABC123", SEDE)).isFalse();
    }

    @Test
    @DisplayName("CP-HU-17-X02 / CA-05: el pase de otra sede no permite reingreso sin cobro")
    void paseDeOtraSede() {
        stored.add(pass(1L, "ABC123", OTRA_SEDE, LocalDate.of(2026, 11, 23)));

        PassResult result = service.find("ABC123", SEDE);

        assertThat(result.status()).isEqualTo(PassStatus.OTRA_SEDE);
        assertThat(result.allowsFreeReentry()).isFalse();
        assertThat(service.hasValidPass("ABC123", SEDE)).isFalse();
    }

    @Test
    @DisplayName("Sin cobro pagado la consulta responde PassNotFound y el reingreso no es gratis")
    void sinPase() {
        assertThatThrownBy(() -> service.find("ABC123", SEDE)).isInstanceOf(PassNotFoundException.class);
        assertThat(service.hasValidPass("ABC123", SEDE)).isFalse();
    }

    @Test
    @DisplayName("Una placa con formato inválido se rechaza")
    void placaInvalida() {
        assertThatThrownBy(() -> service.find("12", SEDE)).isInstanceOf(InvalidPlateException.class);
    }

    private static Pass pass(long id, String plate, long site, LocalDate operatingDay) {
        return new Pass(id, plate, site, operatingDay,
                operatingDay.plusDays(1).atTime(6, 0).atZone(BOGOTA).toOffsetDateTime());
    }

    /** Misma regla que el SQL: primero la sede pedida, luego el pase más reciente. */
    private class InMemoryPasses implements PassRepository {
        @Override
        public Optional<Pass> findLatest(String plate, long preferredSiteId) {
            return stored.stream()
                    .filter(pass -> pass.plate().equals(plate))
                    .max(Comparator.comparing((Pass pass) -> pass.siteId() == preferredSiteId)
                            .thenComparing(Pass::validUntil));
        }
    }
}
