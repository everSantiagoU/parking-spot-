package com.parkingspot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.application.dto.PaymentResult;
import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.service.CashPaymentService;
import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.domain.exception.PaymentRejectedException.Reason;
import com.parkingspot.domain.model.Charge;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.Payment;
import com.parkingspot.domain.model.PaymentMethod;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.port.out.PayableChargeRepository;
import com.parkingspot.domain.port.out.PaymentRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.state.ChargeStates;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del pago en efectivo idempotente (HU-16). */
class CashPaymentServiceTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final Map<Long, Charge> charges = new HashMap<>();
    private final Map<Long, OffsetDateTime> passes = new HashMap<>();
    private final List<Payment> payments = new ArrayList<>();
    private CashPaymentService service;

    @BeforeEach
    void setUp() {
        charges.put(1L, charge(1L, ChargeStatus.PENDIENTE));
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 11, 23, 15, 0).atZone(BOGOTA).toInstant(), BOGOTA);
        Transactions direct = new Transactions() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return work.get();
            }
        };
        service = new CashPaymentService(new InMemoryCharges(), new InMemoryPayments(), new ChargeStates(),
                new OperatingDayCalculator(BOGOTA), direct, clock);
    }

    @Test
    @DisplayName("CP-HU-16-E01 / CA-01: pagar el importe correcto deja el cobro PAGADO")
    void pagoExitoso() {
        PaymentResult result = service.payInCash(1L, new BigDecimal("10000"), "clave-1", "operador");

        assertThat(result.replayed()).isFalse();
        assertThat(result.chargeStatus()).isEqualTo(ChargeStatus.PAGADO);
        assertThat(charges.get(1L).status()).isEqualTo(ChargeStatus.PAGADO);
        assertThat(payments).hasSize(1);
    }

    @Test
    @DisplayName("CA-03: el pago guarda importe, fecha, método y operador")
    void trazabilidad() {
        service.payInCash(1L, new BigDecimal("10000"), "clave-1", "operador");

        Payment payment = service.findByCharge(1L);
        assertThat(payment.amount()).isEqualByComparingTo("10000");
        assertThat(payment.method()).isEqualTo(PaymentMethod.EFECTIVO);
        assertThat(payment.operator()).isEqualTo("operador");
        assertThat(payment.registeredAt().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 23, 15, 0));
    }

    @Test
    @DisplayName("CA-08: el pase queda vigente hasta las 06:00 del día siguiente")
    void paseVigente() {
        PaymentResult result = service.payInCash(1L, new BigDecimal("10000"), "clave-1", "operador");

        assertThat(result.passValidUntil().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 24, 6, 0));
        assertThat(passes.get(1L)).isEqualTo(result.passValidUntil());
    }

    @Test
    @DisplayName("CP-HU-16-E02 / CA-02: repetir la solicitud devuelve el pago original")
    void idempotencia() {
        PaymentResult first = service.payInCash(1L, new BigDecimal("10000"), "clave-1", "operador");
        PaymentResult again = service.payInCash(1L, new BigDecimal("10000.00"), "clave-1", "operador");

        assertThat(again.replayed()).isTrue();
        assertThat(again.payment().id()).isEqualTo(first.payment().id());
        assertThat(payments).hasSize(1);
    }

    @Test
    @DisplayName("CP-HU-16-X02: la misma clave con otros datos es un conflicto y no altera el pago")
    void claveConOtrosDatos() {
        service.payInCash(1L, new BigDecimal("10000"), "clave-1", "operador");

        assertRejected(() -> service.payInCash(1L, new BigDecimal("9000"), "clave-1", "operador"),
                Reason.IDEMPOTENCIA_CONFLICTO);
        assertThat(payments).singleElement().extracting(Payment::amount).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("CA-06: un cobro pagado no admite otro pago con otra clave")
    void pagoDuplicado() {
        service.payInCash(1L, new BigDecimal("10000"), "clave-1", "operador");

        assertRejected(() -> service.payInCash(1L, new BigDecimal("10000"), "clave-2", "operador"),
                Reason.COBRO_YA_PAGADO);
        assertThat(payments).hasSize(1);
    }

    @Test
    @DisplayName("CA-04 / CP-HU-16-X01: un cobro inexistente se rechaza")
    void cobroInexistente() {
        assertRejected(() -> service.payInCash(99L, new BigDecimal("10000"), "clave-1", "operador"),
                Reason.COBRO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("CA-05 / CP-HU-16-X01: un importe distinto al pendiente se rechaza y nada cambia")
    void importeIncorrecto() {
        assertRejected(() -> service.payInCash(1L, new BigDecimal("9999"), "clave-1", "operador"),
                Reason.IMPORTE_INCORRECTO);
        assertThat(charges.get(1L).status()).isEqualTo(ChargeStatus.PENDIENTE);
        assertThat(payments).isEmpty();
    }

    @Test
    void cobroAnuladoNoSePaga() {
        charges.put(2L, charge(2L, ChargeStatus.ANULADO));

        assertRejected(() -> service.payInCash(2L, new BigDecimal("10000"), "clave-1", "operador"),
                Reason.COBRO_NO_PAGABLE);
    }

    @Test
    void solicitudSinClaveOImporte() {
        assertRejected(() -> service.payInCash(1L, new BigDecimal("10000"), " ", "operador"),
                Reason.SOLICITUD_INVALIDA);
        assertRejected(() -> service.payInCash(1L, BigDecimal.ZERO, "clave-1", "operador"),
                Reason.SOLICITUD_INVALIDA);
    }

    private static void assertRejected(ThrowingCallable call, Reason reason) {
        assertThatThrownBy(call)
                .isInstanceOf(PaymentRejectedException.class)
                .extracting(error -> ((PaymentRejectedException) error).reason())
                .isEqualTo(reason);
    }

    private static Charge charge(long id, ChargeStatus status) {
        return new Charge(id, 1L, "ABC123", VehicleType.CARRO, LocalDate.of(2026, 11, 23), TimeSlot.DIURNA, 1L,
                new BigDecimal("10000.00"), new BigDecimal("10000.00"), status, OffsetDateTime.now());
    }

    private class InMemoryCharges implements PayableChargeRepository {
        @Override
        public Optional<Charge> lockById(long chargeId) {
            return Optional.ofNullable(charges.get(chargeId));
        }

        @Override
        public void updateStatus(long chargeId, ChargeStatus status, OffsetDateTime validUntil) {
            Charge c = charges.get(chargeId);
            charges.put(chargeId, new Charge(c.id(), c.siteId(), c.plate(), c.vehicleType(), c.operatingDay(),
                    c.timeSlot(), c.tariffId(), c.appliedTariff(), c.amount(), status, c.generatedAt()));
            passes.put(chargeId, validUntil);
        }
    }

    private class InMemoryPayments implements PaymentRepository {
        @Override
        public Optional<Payment> findByIdempotencyKey(String key) {
            return payments.stream().filter(p -> p.idempotencyKey().equals(key)).findFirst();
        }

        @Override
        public Optional<Payment> findByChargeId(long chargeId) {
            return payments.stream().filter(p -> p.chargeId() == chargeId).findFirst();
        }

        @Override
        public Payment save(Payment payment) {
            Payment saved = new Payment((long) payments.size() + 1, payment.chargeId(), payment.method(),
                    payment.amount(), payment.operator(), payment.idempotencyKey(), payment.registeredAt());
            payments.add(saved);
            return saved;
        }
    }
}
