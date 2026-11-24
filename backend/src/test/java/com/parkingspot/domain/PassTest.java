package com.parkingspot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkingspot.domain.model.Pass;
import com.parkingspot.domain.model.PassStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Vigencia del pase (HU-17). Pase del día operativo 23/11/2026, vigente hasta el 24/11 a las 06:00. */
class PassTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final long SEDE = 1L;
    private static final long OTRA_SEDE = 2L;

    private final Pass pass = new Pass(7L, "ABC123", SEDE, LocalDate.of(2026, 11, 23),
            LocalDateTime.of(2026, 11, 24, 6, 0).atZone(BOGOTA).toOffsetDateTime());

    @Test
    @DisplayName("CA-02: el mismo día operativo el pase está vigente")
    void vigenteElMismoDia() {
        assertThat(pass.statusAt(at(2026, 11, 23, 20, 0), SEDE)).isEqualTo(PassStatus.VIGENTE);
    }

    @Test
    @DisplayName("CA-02: a las 05:59 del día siguiente el pase sigue vigente")
    void vigenteAntesDeLasSeis() {
        assertThat(pass.statusAt(at(2026, 11, 24, 5, 59), SEDE)).isEqualTo(PassStatus.VIGENTE);
    }

    @Test
    @DisplayName("CP-HU-17-X01 / CA-03: desde las 06:00 del día siguiente el pase está vencido")
    void vencidoDesdeLasSeis() {
        assertThat(pass.statusAt(at(2026, 11, 24, 6, 0), SEDE)).isEqualTo(PassStatus.VENCIDO);
        assertThat(pass.statusAt(at(2026, 11, 25, 12, 0), SEDE)).isEqualTo(PassStatus.VENCIDO);
    }

    @Test
    @DisplayName("CP-HU-17-X02 / CA-05: un pase de otra sede no habilita la exención")
    void otraSede() {
        Instant duringDay = at(2026, 11, 23, 20, 0);

        assertThat(pass.statusAt(duringDay, OTRA_SEDE)).isEqualTo(PassStatus.OTRA_SEDE);
        assertThat(pass.allowsFreeReentry(duringDay, OTRA_SEDE)).isFalse();
    }

    @Test
    @DisplayName("CA-04: solo el pase vigente de la misma sede permite reingresar sin cobro")
    void reingresoSinCobro() {
        assertThat(pass.allowsFreeReentry(at(2026, 11, 23, 20, 0), SEDE)).isTrue();
        assertThat(pass.allowsFreeReentry(at(2026, 11, 24, 6, 0), SEDE)).isFalse();
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(BOGOTA).toInstant();
    }
}
