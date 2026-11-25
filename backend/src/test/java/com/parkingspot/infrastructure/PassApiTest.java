package com.parkingspot.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

/** HU-17 de punta a punta con PostgreSQL real. Reloj fijo: 23/11/2026 a las 15:00 de Bogotá. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(PassApiTest.FixedClock.class)
class PassApiTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String OPERATOR = "operador.pases";
    private static final String OTHER_SITE = "Sede Norte";

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

    @BeforeEach
    void limpiar() {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("DELETE FROM pagos").update();
        jdbc.sql("DELETE FROM cobros").update();
        jdbc.sql("DELETE FROM usuarios").update();
        jdbc.sql("DELETE FROM sedes WHERE nombre = :name").param("name", OTHER_SITE).update();
        jdbc.sql("UPDATE tarifas SET valor = CASE franja WHEN 'DIURNA' THEN 10000 ELSE 6000 END WHERE tipo_vehiculo = 'CARRO'").update();
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT :username, 'sin-uso', 'Operador de pases', r.id, s.id
                FROM roles r, sedes s WHERE r.nombre = 'OPERADOR' AND s.nombre = 'Sede Principal'
                """).param("username", OPERATOR).update();
    }

    @Test
    @DisplayName("CP-HU-17-E01 / CA-01 / CA-04: pagar, salir y reingresar el mismo día no genera otro cobro")
    void reingresoSinCobro() throws Exception {
        charge("ABC123").andExpect(status().isCreated());
        long chargeId = jdbc.sql("SELECT id FROM cobros WHERE placa = 'ABC123'").query(Long.class).single();
        pay(chargeId).andExpect(status().isCreated());

        findPass("ABC123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("ABC123"))
                .andExpect(jsonPath("$.chargeId").value(chargeId))
                .andExpect(jsonPath("$.operatingDay").value("2026-11-23"))
                .andExpect(jsonPath("$.validUntil").exists())
                .andExpect(jsonPath("$.status").value("VIGENTE"))
                .andExpect(jsonPath("$.freeReentry").value(true));

        charge("ABC123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("EXISTENTE"))
                .andExpect(jsonPath("$.status").value("PAGADO"));
        assertThat(chargesOf("ABC123")).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-HU-17-X01 / CA-03: el pase de un día operativo anterior se consulta como vencido")
    void paseVencido() throws Exception {
        insertPaidCharge("XYZ789", "Sede Principal", LocalDate.of(2026, 11, 22));

        findPass("XYZ789")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VENCIDO"))
                .andExpect(jsonPath("$.freeReentry").value(false));
    }

    @Test
    @DisplayName("CP-HU-17-X02 / CA-05: el pase de otra sede no aplica la exención y el ingreso sí genera cobro")
    void paseDeOtraSede() throws Exception {
        jdbc.sql("INSERT INTO sedes (nombre) VALUES (:name)").param("name", OTHER_SITE).update();
        insertPaidCharge("DEF456", OTHER_SITE, LocalDate.of(2026, 11, 23));

        findPass("DEF456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTRA_SEDE"))
                .andExpect(jsonPath("$.freeReentry").value(false));

        charge("DEF456")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("CREADO"));
        assertThat(chargesOf("DEF456")).isEqualTo(2);
    }

    @Test
    @DisplayName("Un cobro pendiente no es un pase: responde 404 PASE_NO_ENCONTRADO; placa inválida 400")
    void sinPase() throws Exception {
        charge("GHI789").andExpect(status().isCreated());

        findPass("GHI789")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PASE_NO_ENCONTRADO"));
        findPass("12")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sin sesión responde 401 y sin rol de operador 403")
    void autorizacion() throws Exception {
        mvc.perform(get("/api/v1/passes").param("plate", "ABC123"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/passes").param("plate", "ABC123").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    private ResultActions findPass(String plate) throws Exception {
        return mvc.perform(get("/api/v1/passes").param("plate", plate).with(user(OPERATOR).roles("OPERADOR")));
    }

    private ResultActions charge(String plate) throws Exception {
        return mvc.perform(post("/api/v1/charges").with(user(OPERATOR).roles("OPERADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"%s\"}".formatted(plate)));
    }

    private ResultActions pay(long chargeId) throws Exception {
        return mvc.perform(post("/api/v1/payments").with(user(OPERATOR).roles("OPERADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"chargeId\":%d,\"amount\":10000,\"idempotencyKey\":\"pase-%d\"}".formatted(chargeId, chargeId)));
    }

    /** Cobro ya pagado con su pase vigente hasta las 06:00 del día siguiente (como lo deja HU-16). */
    private void insertPaidCharge(String plate, String site, LocalDate operatingDay) {
        jdbc.sql("""
                INSERT INTO cobros (sede_id, placa, tipo_vehiculo, dia_operativo, franja, tarifa_id,
                                    tarifa_aplicada, valor, estado, vigente_hasta)
                SELECT s.id, :plate, 'CARRO', :day, 'DIURNA', t.id, 10000, 10000, 'PAGADO', :validUntil
                FROM sedes s,
                     (SELECT id FROM tarifas WHERE tipo_vehiculo = 'CARRO' AND franja = 'DIURNA' LIMIT 1) t
                WHERE s.nombre = :site
                """)
                .param("plate", plate)
                .param("day", operatingDay)
                .param("validUntil", operatingDay.plusDays(1).atTime(6, 0).atZone(BOGOTA).toOffsetDateTime())
                .param("site", site)
                .update();
    }

    private long chargesOf(String plate) {
        return jdbc.sql("SELECT COUNT(*) FROM cobros WHERE placa = :plate").param("plate", plate)
                .query(Long.class).single();
    }
}
