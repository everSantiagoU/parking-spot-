package com.parkingspot.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkingspot.application.dto.ChargeResult;
import com.parkingspot.application.usecase.GenerateChargeUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HU-14 y HU-15 de punta a punta: API REST, seguridad, migraciones Flyway y PostgreSQL real.
 * El reloj queda fijo en el 23/11/2026 a las 10:00 de Bogotá (franja diurna).
 * Si Docker no está disponible, la clase se omite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(ChargeApiTest.FixedClock.class)
class ChargeApiTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String OPERATOR = "operador.cobros";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedParkingClock() {
            return Clock.fixed(LocalDateTime.of(2026, 11, 23, 10, 0).atZone(BOGOTA).toInstant(), BOGOTA);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private GenerateChargeUseCase generateCharge;

    @BeforeEach
    void datosLimpios() {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("DELETE FROM pagos").update();
        jdbc.sql("DELETE FROM cobros").update();
        jdbc.sql("DELETE FROM usuarios").update();
        jdbc.sql("UPDATE tarifas SET valor = 10000 WHERE tipo_vehiculo = 'CARRO' AND franja = 'DIURNA'").update();
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT :username, 'sin-uso', 'Operador de cobros', r.id, s.id
                FROM roles r, sedes s WHERE r.nombre = 'OPERADOR' AND s.nombre = 'Sede Principal'
                """).param("username", OPERATOR).update();
    }

    @Test
    @DisplayName("CP-HU-14-E01: un carro en franja diurna genera un cobro PENDIENTE de $10.000")
    void carroDiurnoGeneraCobro() throws Exception {
        mvc.perform(asOperator(post("/api/v1/charges")).content("{\"plate\":\"abc123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("ABC123"))
                .andExpect(jsonPath("$.vehicleType").value("CARRO"))
                .andExpect(jsonPath("$.operatingDay").value("2026-11-23"))
                .andExpect(jsonPath("$.timeSlot").value("DIURNA"))
                .andExpect(jsonPath("$.outcome").value("CREADO"))
                .andExpect(jsonPath("$.amount").value(10000.0))
                .andExpect(jsonPath("$.status").value("PENDIENTE"));
    }

    @Test
    @DisplayName("HU-15 CA-03: el reingreso devuelve el mismo cobro y no crea otro")
    void reingresoNoDuplica() throws Exception {
        mvc.perform(asOperator(post("/api/v1/charges")).content("{\"plate\":\"ABC123\"}"))
                .andExpect(status().isCreated());
        mvc.perform(asOperator(post("/api/v1/charges")).content("{\"plate\":\"ABC123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("EXISTENTE"));

        assertThat(countCharges("ABC123")).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-HU-14-E02: una moto con tarifa $0 queda EXENTA y no se registra cobro")
    void motoExenta() throws Exception {
        mvc.perform(asOperator(post("/api/v1/charges")).content("{\"plate\":\"ABC12D\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("EXENTO"))
                .andExpect(jsonPath("$.amount").value(0));

        assertThat(countCharges("ABC12D")).isZero();
        mvc.perform(asOperator(get("/api/v1/charges/today").param("plate", "ABC12D")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COBRO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("HU-14 CA-04: tras cambiar la tarifa, el cobro existente conserva su valor")
    void tarifaConservada() throws Exception {
        mvc.perform(asOperator(post("/api/v1/charges")).content("{\"plate\":\"ABC123\"}"))
                .andExpect(status().isCreated());
        jdbc.sql("UPDATE tarifas SET valor = 12000 WHERE tipo_vehiculo = 'CARRO' AND franja = 'DIURNA'").update();

        mvc.perform(asOperator(get("/api/v1/charges/today").param("plate", "ABC123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(10000.0))
                .andExpect(jsonPath("$.appliedTariff").value(10000.0));
    }

    @Test
    @DisplayName("CP-HU-14-X01: sin tarifa configurada responde 422 y no registra nada")
    void sinTarifa() throws Exception {
        jdbc.sql("DELETE FROM tarifas WHERE tipo_vehiculo = 'CARRO' AND franja = 'DIURNA'").update();
        try {
            mvc.perform(asOperator(post("/api/v1/charges")).content("{\"plate\":\"ABC123\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("TARIFA_NO_CONFIGURADA"));
            assertThat(countCharges("ABC123")).isZero();
        } finally {
            jdbc.sql("""
                    INSERT INTO tarifas (sede_id, tipo_vehiculo, franja, valor)
                    SELECT id, 'CARRO', 'DIURNA', 10000 FROM sedes WHERE nombre = 'Sede Principal'
                    """).update();
        }
    }

    @Test
    @DisplayName("Placa inválida responde 400")
    void placaInvalida() throws Exception {
        mvc.perform(asOperator(post("/api/v1/charges")).content("{\"plate\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLATE"));
    }

    @Test
    @DisplayName("Solo el rol OPERADOR puede generar cobros")
    void soloOperador() throws Exception {
        mvc.perform(post("/api/v1/charges").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"ABC123\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/charges").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"ABC123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HU-15 CA-06: con solicitudes simultáneas queda un solo cobro en PostgreSQL")
    void concurrencia() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ChargeResult>> results = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return generateCharge.generate("RAC123", siteId());
            }));
        }
        start.countDown();
        long created = 0;
        for (Future<ChargeResult> result : results) {
            if (result.get().outcome() == ChargeResult.Outcome.CREADO) {
                created++;
            }
        }
        pool.shutdown();

        assertThat(created).isEqualTo(1);
        assertThat(countCharges("RAC123")).isEqualTo(1);
    }

    private MockHttpServletRequestBuilder asOperator(MockHttpServletRequestBuilder request) {
        return request.with(user(OPERATOR).roles("OPERADOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    private long countCharges(String plate) {
        return jdbc.sql("SELECT COUNT(*) FROM cobros WHERE placa = :plate").param("plate", plate)
                .query(Long.class).single();
    }

    private long siteId() {
        return jdbc.sql("SELECT id FROM sedes WHERE nombre = 'Sede Principal'").query(Long.class).single();
    }
}
