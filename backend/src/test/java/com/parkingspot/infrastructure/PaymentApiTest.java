package com.parkingspot.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** HU-16 de punta a punta con PostgreSQL real. Reloj fijo: 23/11/2026 a las 15:00 de Bogotá. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(PaymentApiTest.FixedClock.class)
class PaymentApiTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String OPERATOR = "operador.caja";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedParkingClock() {
            return Clock.fixed(LocalDateTime.of(2026, 11, 23, 15, 0).atZone(BOGOTA).toInstant(), BOGOTA);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private long chargeId;

    @BeforeEach
    void cobroPendiente() throws Exception {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("DELETE FROM pagos").update();
        jdbc.sql("DELETE FROM cobros").update();
        jdbc.sql("DELETE FROM usuarios").update();
        jdbc.sql("UPDATE tarifas SET valor = CASE franja WHEN 'DIURNA' THEN 10000 ELSE 6000 END WHERE tipo_vehiculo = 'CARRO'").update();
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT :username, 'sin-uso', 'Operador de caja', r.id, s.id
                FROM roles r, sedes s WHERE r.nombre = 'OPERADOR' AND s.nombre = 'Sede Principal'
                """).param("username", OPERATOR).update();
        // El cobro se genera con el motor de cobros de HU-14 (carro diurno, $10.000).
        mvc.perform(post("/api/v1/charges").with(user(OPERATOR).roles("OPERADOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"ABC123\"}"))
                .andExpect(status().isCreated());
        chargeId = jdbc.sql("SELECT id FROM cobros WHERE placa = 'ABC123'").query(Long.class).single();
    }

    @Test
    @DisplayName("CP-HU-16-E01 / CA-01 / CA-08: el pago deja el cobro PAGADO y el pase vigente en PostgreSQL")
    void pagoExitoso() throws Exception {
        pay(chargeId, "10000", "clave-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chargeStatus").value("PAGADO"))
                .andExpect(jsonPath("$.payment.method").value("EFECTIVO"))
                .andExpect(jsonPath("$.payment.operator").value(OPERATOR))
                .andExpect(jsonPath("$.replayed").value(false));

        assertThat(jdbc.sql("SELECT estado FROM cobros WHERE id = :id").param("id", chargeId)
                .query(String.class).single()).isEqualTo("PAGADO");
        OffsetDateTime validUntil = jdbc.sql("SELECT vigente_hasta FROM cobros WHERE id = :id").param("id", chargeId)
                .query(OffsetDateTime.class).single();
        assertThat(validUntil.atZoneSameInstant(BOGOTA).toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 24, 6, 0));
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-HU-16-E02 / CA-02: repetir la misma solicitud conserva un único pago")
    void idempotencia() throws Exception {
        pay(chargeId, "10000", "clave-1").andExpect(status().isCreated());
        pay(chargeId, "10000", "clave-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));

        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-HU-16-X02: la misma clave con otro importe responde 409 sin alterar el pago")
    void claveConOtrosDatos() throws Exception {
        pay(chargeId, "10000", "clave-1").andExpect(status().isCreated());
        pay(chargeId, "5000", "clave-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCIA_CONFLICTO"));

        assertThat(jdbc.sql("SELECT valor FROM pagos").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("CA-06: pagar otra vez con otra clave responde 409 COBRO_YA_PAGADO")
    void pagoDuplicado() throws Exception {
        pay(chargeId, "10000", "clave-1").andExpect(status().isCreated());
        pay(chargeId, "10000", "clave-2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COBRO_YA_PAGADO"));

        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-04 y CA-05: cobro inexistente responde 404 e importe incorrecto 400")
    void erroresControlados() throws Exception {
        pay(999_999L, "10000", "clave-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COBRO_NO_ENCONTRADO"));
        pay(chargeId, "9000", "clave-2")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORTE_INCORRECTO"));

        assertThat(paymentCount()).isZero();
    }

    @Test
    @DisplayName("CA-03: la consulta del pago muestra importe, fecha, método y operador")
    void trazabilidad() throws Exception {
        pay(chargeId, "10000", "clave-1").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/payments/charge/" + chargeId).with(user(OPERATOR).roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(10000.0))
                .andExpect(jsonPath("$.method").value("EFECTIVO"))
                .andExpect(jsonPath("$.operator").value(OPERATOR))
                .andExpect(jsonPath("$.registeredAt").exists());
    }

    @Test
    @DisplayName("CA-07: sin sesión responde 401 y sin rol de operador 403")
    void autorizacion() throws Exception {
        mvc.perform(post("/api/v1/payments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(chargeId, "10000", "clave-1")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/payments").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(chargeId, "10000", "clave-1")))
                .andExpect(status().isForbidden());

        assertThat(paymentCount()).isZero();
    }

    @Test
    @DisplayName("Concurrencia: seis pagos simultáneos con claves distintas dejan un solo pago")
    void concurrencia() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String key = "concurrente-" + i;
            statuses.add(pool.submit(() -> {
                start.await();
                return pay(chargeId, "10000", key).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        long created = 0;
        for (Future<Integer> status : statuses) {
            if (status.get() == 201) {
                created++;
            }
        }
        pool.shutdown();

        assertThat(created).isEqualTo(1);
        assertThat(paymentCount()).isEqualTo(1);
    }

    private ResultActions pay(long charge, String amount, String key) throws Exception {
        return mvc.perform(post("/api/v1/payments").with(user(OPERATOR).roles("OPERADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body(charge, amount, key)));
    }

    private static String body(long charge, String amount, String key) {
        return "{\"chargeId\":%d,\"amount\":%s,\"idempotencyKey\":\"%s\"}".formatted(charge, amount, key);
    }

    private long paymentCount() {
        return jdbc.sql("SELECT COUNT(*) FROM pagos").query(Long.class).single();
    }
}
