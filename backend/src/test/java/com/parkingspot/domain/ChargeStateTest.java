package com.parkingspot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.domain.exception.PaymentRejectedException.Reason;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.service.state.ChargeStates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Patrón State: cada estado del cobro decide si admite un pago. */
class ChargeStateTest {
    private final ChargeStates states = new ChargeStates();

    @Test
    @DisplayName("Un cobro PENDIENTE pasa a PAGADO")
    void pendientePasaAPagado() {
        assertThat(states.of(ChargeStatus.PENDIENTE).pay(charge(ChargeStatus.PENDIENTE)))
                .isEqualTo(ChargeStatus.PAGADO);
    }

    @ParameterizedTest(name = "{0} rechaza el pago con {1}")
    @CsvSource({"PAGADO, COBRO_YA_PAGADO", "VENCIDO, COBRO_NO_PAGABLE", "ANULADO, COBRO_NO_PAGABLE"})
    void otrosEstadosRechazanElPago(ChargeStatus status, Reason reason) {
        assertThatThrownBy(() -> states.of(status).pay(charge(status)))
                .isInstanceOf(PaymentRejectedException.class)
                .extracting(error -> ((PaymentRejectedException) error).reason())
                .isEqualTo(reason);
    }

    private static Charge charge(ChargeStatus status) {
        return new Charge(1L, 1L, "ABC123", VehicleType.CARRO, LocalDate.of(2026, 11, 23), TimeSlot.DIURNA, 1L,
                new BigDecimal("10000"), new BigDecimal("10000"), status, OffsetDateTime.now());
    }
}
