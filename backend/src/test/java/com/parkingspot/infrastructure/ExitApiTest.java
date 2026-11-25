package com.parkingspot.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingspot.domain.model.ExitDecision;
import com.parkingspot.domain.port.out.ExitBarrier;
import com.parkingspot.infrastructure.adapter.in.rest.ExitController.ExitResponse;
import com.parkingspot.infrastructure.adapter.out.persistence.JdbcChargeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(ChargeApiTest.FixedClock.class)
class ExitApiTest {
    private static final String EXIT = "/api/v1/exits/validate";
    private static final String OCCUPANCY = "/api/v1/occupancy";
    private static final String OPERATOR = "operador.salida";
    private static final LocalDate DAY = LocalDate.of(2026, 11, 23);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean ExitBarrier barrier;
    @MockitoSpyBean JdbcChargeRepository charges;
    private long siteId;

    @BeforeEach
    void reset() {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("DELETE FROM pagos").update();
        jdbc.sql("DELETE FROM cobros").update();
        jdbc.sql("DELETE FROM usuarios").update();
        jdbc.sql("""
                UPDATE ocupacion SET ocupados = 0, capacidad = CASE tipo_vehiculo WHEN 'CARRO' THEN 50 ELSE 30 END
                """).update();
        jdbc.sql("UPDATE tarifas SET valor = 0 WHERE tipo_vehiculo = 'MOTO'").update();
        siteId = jdbc.sql("SELECT id FROM sedes WHERE nombre = 'Sede Principal'").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT :username, 'sin-uso', 'Operador de salida', id, :site FROM roles WHERE nombre = 'OPERADOR'
                """).param("username", OPERATOR).param("site", siteId).update();
    }

    @Test
    void simultaneousEntriesOccupyOneSpotUntilEffectiveExit() throws Exception {
        var start = new CyclicBarrier(2);
        Callable<String> enter = () -> {
            start.await(10, TimeUnit.SECONDS);
            var response = mvc.perform(asOperator("/api/v1/entries/manual", " abc12d "))
                    .andReturn().getResponse();
            var body = json.readTree(response.getContentAsByteArray());
            boolean authorized = body.path("barreraAbierta").asBoolean();
            assertThat(response.getStatus()).isEqualTo(authorized ? 201 : 409);
            return body.path("resultado").asText();
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(enter);
            var second = pool.submit(enter);
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("AUTORIZADO", "INGRESO_ACTIVO");
        }
        assertThat(occupied("MOTO")).isOne();
        expectDecision("ABC12D", true, "EXENCION_COMPROBADA");
        assertThat(occupied("MOTO")).isZero();
        enter("ABC12D");
        assertThat(occupied("MOTO")).isOne();
    }

    @Test
    void latestPlateIgnoresARejectedEntry() throws Exception {
        enter("ABC12D");
        jdbc.sql("UPDATE ocupacion SET capacidad = 0 WHERE tipo_vehiculo = 'CARRO'").update();
        mvc.perform(asOperator("/api/v1/entries/manual", "XYZ123")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("LLENO"));
        mvc.perform(get("/api/v1/entries/latest").with(user(OPERATOR).roles("OPERADOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plate").value("ABC12D"));
    }

    @Test
    void paidExitReleasesExactlyOneOfTenAndAuditsTheDecision() throws Exception {
        jdbc.sql("UPDATE ocupacion SET capacidad = 10 WHERE tipo_vehiculo = 'CARRO'").update();
        for (int i = 0; i < 9; i++) {
            enter("CAR00" + i);
        }
        enter("ABC12D");
        enter("ABC123");
        chargeCar();
        markPaid();
        assertThat(occupied("CARRO")).isEqualTo(10);
        assertThat(occupied("MOTO")).isEqualTo(1);

        expectDecision(" abc123 ", true, "PAGO_CONFIRMADO");
        verify(barrier).open(siteId, "ABC123");
        var attempts = jdbc.sql("""
                SELECT e.placa, e.sede_id, e.resultado, e.motivo, e.cobro_id, e.ocurrido_en, u.username
                FROM eventos e JOIN usuarios u ON u.id = e.usuario_id WHERE tipo_evento = 'VALIDACION'
                """).query((row, index) -> {
                    assertThat(row.getString("placa")).isEqualTo("ABC123");
                    assertThat(row.getLong("sede_id")).isEqualTo(siteId);
                    assertThat(row.getString("resultado")).isEqualTo("AUTORIZADO");
                    assertThat(row.getString("motivo")).isEqualTo("PAGO_CONFIRMADO");
                    assertThat(row.getObject("cobro_id")).isNotNull();
                    assertThat(row.getObject("ocurrido_en", OffsetDateTime.class).toInstant())
                            .isEqualTo(Instant.parse("2026-11-23T15:00:00Z"));
                    return row.getString("username");
                }).list();
        assertThat(attempts).containsExactly(OPERATOR);
        assertThat(occupied("CARRO")).isEqualTo(9);
        assertThat(occupied("MOTO")).isEqualTo(1);
        assertThat(consumedEntries()).containsExactly(jdbc.sql("""
                SELECT id FROM eventos WHERE tipo_evento = 'INGRESO' AND placa = 'ABC123'
                """).query(Long.class).single());
        mvc.perform(get(OCCUPANCY).with(user(OPERATOR).roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleType").value("CARRO"))
                .andExpect(jsonPath("$[0].capacity").value(10))
                .andExpect(jsonPath("$[0].occupied").value(9))
                .andExpect(jsonPath("$[1].vehicleType").value("MOTO"))
                .andExpect(jsonPath("$[1].occupied").value(1));
    }

    @Test
    void cpE02ExemptionSurvivesTariffChange() throws Exception {
        enter("ABC12D");
        jdbc.sql("UPDATE tarifas SET valor = 2000 WHERE tipo_vehiculo = 'MOTO'").update();

        expectDecision("ABC12D", true, "EXENCION_COMPROBADA");
        verify(barrier).open(siteId, "ABC12D");
        assertThat(occupied("MOTO")).isZero();
        assertThat(consumedEntries()).hasSize(1);
    }

    @Test
    void cpX01PendingChargeDeniesAndAudits() throws Exception {
        enter("ABC123");
        chargeCar();

        expectDecision("ABC123", false, "PAGO_PENDIENTE");
        assertThat(jdbc.sql("""
                SELECT motivo FROM eventos WHERE tipo_evento = 'VALIDACION' AND resultado = 'RECHAZADO'
                """).query(String.class).single()).isEqualTo("PAGO_PENDIENTE");
        verifyNoInteractions(barrier);
        assertThat(occupancy()).isEqualTo(1);
        assertThat(consumedEntries()).isEmpty();

        markPaid();
        expectDecision("ABC123", true, "PAGO_CONFIRMADO");
        assertThat(occupied("CARRO")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VENCIDO", "ANULADO"})
    void closedChargeDoesNotSatisfyTheObligation(String chargeStatus) throws Exception {
        enter("ABC123");
        chargeCar();
        jdbc.sql("UPDATE cobros SET estado = :status").param("status", chargeStatus).update();

        expectDecision("ABC123", false, "COBRO_NO_SATISFECHO");
        verifyNoInteractions(barrier);
    }

    @Test
    void missingEntryDeniesEvenWithPaidCharge() throws Exception {
        chargeCar();
        jdbc.sql("UPDATE cobros SET estado = 'PAGADO', vigente_hasta = generado_en").update();

        expectDecision("ABC123", false, "SIN_INGRESO_ACTIVO");
        assertThat(jdbc.sql("SELECT motivo FROM eventos").query(String.class).single())
                .isEqualTo("SIN_INGRESO_ACTIVO");
        assertThat(occupancy()).isZero();
        verifyNoInteractions(barrier);
    }

    @Test
    void entryInAnotherSiteDoesNotAuthorize() throws Exception {
        jdbc.sql("INSERT INTO sedes (nombre) VALUES ('Otra sede') ON CONFLICT (nombre) DO NOTHING").update();
        long otherSite = jdbc.sql("SELECT id FROM sedes WHERE nombre = 'Otra sede'").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO eventos (sede_id, placa, tipo_vehiculo, tipo_evento, origen_lectura, resultado,
                                     dia_operativo_exento)
                VALUES (:site, 'ABC12D', 'MOTO', 'INGRESO', 'AUTOMATICA', 'AUTORIZADO', :day)
                """).param("site", otherSite).param("day", DAY).update();

        expectDecision("ABC12D", false, "SIN_INGRESO_ACTIVO");
        verifyNoInteractions(barrier);
    }

    @Test
    void exemptionIsNotInferredFromNewTariffOrAnotherOperatingDay() throws Exception {
        jdbc.sql("UPDATE tarifas SET valor = 2000 WHERE tipo_vehiculo = 'MOTO'").update();
        enter("ABC12D");
        jdbc.sql("UPDATE tarifas SET valor = 0 WHERE tipo_vehiculo = 'MOTO'").update();
        jdbc.sql("DELETE FROM cobros WHERE placa = 'ABC12D'").update(); // Ingreso histórico sin cobro
        expectDecision("ABC12D", false, "EXENCION_INDETERMINADA");

        enter("XYZ12Z");
        jdbc.sql("""
                UPDATE eventos SET dia_operativo_exento = :yesterday WHERE placa = 'XYZ12Z'
                """).param("yesterday", DAY.minusDays(1)).update();
        expectDecision("XYZ12Z", false, "EXENCION_INDETERMINADA");
        verifyNoInteractions(barrier);
    }

    @Test
    void chargeLookupFailureIsAuditedAndReturns503() throws Exception {
        enter("ABC123");
        doThrow(new DataAccessResourceFailureException("Fallo simulado"))
                .when(charges).findByPlateAndSiteAndDay("ABC123", siteId, DAY);

        mvc.perform(asOperator(EXIT, "ABC123")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.barrierOpen").value(false))
                .andExpect(jsonPath("$.code").value("ERROR_CONSULTA"));
        assertThat(jdbc.sql("SELECT motivo FROM eventos WHERE tipo_evento = 'VALIDACION'")
                .query(String.class).single()).isEqualTo("ERROR_CONSULTA");
        verifyNoInteractions(barrier);
    }

    @Test
    void repeatedExitDoesNotReleaseAnotherSpotAndThePlateCanReenter() throws Exception {
        enter("ABC123");
        enter("XYZ123");
        chargeCar();
        markPaid();

        expectDecision("ABC123", true, "PAGO_CONFIRMADO");
        expectDecision("ABC123", false, "SIN_INGRESO_ACTIVO");
        assertThat(occupied("CARRO")).isEqualTo(1);
        assertThat(consumedEntries()).hasSize(1);
        verify(barrier).open(siteId, "ABC123");

        enter("ABC123");
        expectDecision("ABC123", true, "PAGO_CONFIRMADO");
        assertThat(occupied("CARRO")).isEqualTo(1);
        assertThat(consumedEntries()).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void simultaneousRequestsForTheSameEntryCanCommitOnlyOneExit() throws Exception {
        enter("ABC123");
        enter("XYZ123");
        chargeCar();
        markPaid();

        // Las dos solicitudes ya encontraron el mismo ingreso activo antes de competir por su salida.
        var bothEvaluated = new CyclicBarrier(2);
        doAnswer(invocation -> {
            var charge = invocation.callRealMethod();
            bothEvaluated.await(10, TimeUnit.SECONDS);
            return charge;
        }).when(charges).findByPlateAndSiteAndDay("ABC123", siteId, DAY);

        Callable<ExitResponse> submit = () -> {
            var response = mvc.perform(asOperator(EXIT, "ABC123")).andExpect(status().isOk())
                    .andReturn().getResponse();
            return json.readValue(response.getContentAsByteArray(), ExitResponse.class);
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(submit);
            var second = executor.submit(submit);
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(
                            new ExitResponse(true, ExitDecision.PAGO_CONFIRMADO),
                            new ExitResponse(false, ExitDecision.SIN_INGRESO_ACTIVO));
        }

        assertThat(consumedEntries()).hasSize(1);
        assertThat(occupied("CARRO")).isEqualTo(1);
        assertThat(jdbc.sql("""
                SELECT motivo FROM eventos WHERE tipo_evento = 'VALIDACION'
                """).query(String.class).list()).containsExactlyInAnyOrder("PAGO_CONFIRMADO", "SIN_INGRESO_ACTIVO");
        verify(barrier).open(siteId, "ABC123");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingOrEmptyOccupancyRollsBackTheExitAndKeepsTheEntry(boolean missing) throws Exception {
        enter("ABC12D");
        if (missing) {
            jdbc.sql("DELETE FROM ocupacion WHERE sede_id = :site AND tipo_vehiculo = 'MOTO'")
                    .param("site", siteId).update();
        } else {
            jdbc.sql("UPDATE ocupacion SET ocupados = 0 WHERE sede_id = :site AND tipo_vehiculo = 'MOTO'")
                    .param("site", siteId).update();
        }
        try {
            mvc.perform(asOperator(EXIT, "ABC12D")).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.barrierOpen").value(false))
                    .andExpect(jsonPath("$.code").value("OCUPACION_INCONSISTENTE"));
            assertThat(consumedEntries()).isEmpty();
            assertThat(occupancy()).isZero();
            assertThat(jdbc.sql("SELECT motivo FROM eventos WHERE tipo_evento = 'VALIDACION'")
                    .query(String.class).single()).isEqualTo("OCUPACION_INCONSISTENTE");
            verifyNoInteractions(barrier);
        } finally {
            jdbc.sql("""
                    INSERT INTO ocupacion (sede_id, tipo_vehiculo, capacidad, ocupados) VALUES (:site, 'MOTO', 30, 1)
                    ON CONFLICT (sede_id, tipo_vehiculo) DO UPDATE SET ocupados = 1
                    """).param("site", siteId).update();
        }
        expectDecision("ABC12D", true, "EXENCION_COMPROBADA");
    }

    @Test
    void failureInsertingTheEffectiveExitIsAuditedAndKeepsTheSpotOccupied() throws Exception {
        enter("ABC12D");
        jdbc.sql("ALTER TABLE eventos ADD CONSTRAINT test_exit_failure CHECK (tipo_evento <> 'SALIDA')").update();
        try {
            mvc.perform(asOperator(EXIT, "ABC12D")).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.barrierOpen").value(false))
                    .andExpect(jsonPath("$.code").value("ERROR_REGISTRO_SALIDA"));
            assertThat(jdbc.sql("SELECT motivo FROM eventos WHERE tipo_evento = 'VALIDACION'")
                    .query(String.class).single()).isEqualTo("ERROR_REGISTRO_SALIDA");
            assertThat(occupied("MOTO")).isEqualTo(1);
            assertThat(consumedEntries()).isEmpty();
            verifyNoInteractions(barrier);
        } finally {
            jdbc.sql("ALTER TABLE eventos DROP CONSTRAINT test_exit_failure").update();
        }
        expectDecision("ABC12D", true, "EXENCION_COMPROBADA");
    }

    @Test
    void auditFailureAfterReleaseRollsBackTheExitAndTheOccupancy() throws Exception {
        enter("ABC12D");
        jdbc.sql("ALTER TABLE eventos ADD CONSTRAINT test_audit_failure CHECK (tipo_evento <> 'VALIDACION')").update();
        try {
            mvc.perform(asOperator(EXIT, "ABC12D")).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.barrierOpen").value(false))
                    .andExpect(jsonPath("$.code").value("ERROR_AUDITORIA"));
            verifyNoInteractions(barrier);
            assertThat(occupied("MOTO")).isEqualTo(1);
            assertThat(consumedEntries()).isEmpty();
        } finally {
            jdbc.sql("ALTER TABLE eventos DROP CONSTRAINT test_audit_failure").update();
        }
        expectDecision("ABC12D", true, "EXENCION_COMPROBADA");
        assertThat(occupied("MOTO")).isZero();
    }

    @Test
    void requiresSessionOperatorAndCsrf() throws Exception {
        mvc.perform(request(EXIT, "ABC123").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(request(EXIT, "ABC123").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(request(EXIT, "ABC123").with(user(OPERATOR).roles("OPERADOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(barrier);
    }

    @Test
    void occupancyOnlyIncludesTheSessionOperatorsSite() throws Exception {
        jdbc.sql("INSERT INTO sedes (nombre) VALUES ('Otra sede') ON CONFLICT (nombre) DO NOTHING").update();
        jdbc.sql("""
                INSERT INTO ocupacion (sede_id, tipo_vehiculo, capacidad, ocupados)
                SELECT id, 'CARRO', 20, 15 FROM sedes WHERE nombre = 'Otra sede'
                ON CONFLICT (sede_id, tipo_vehiculo) DO UPDATE SET ocupados = 15
                """).update();
        enter("ABC12D");

        mvc.perform(get(OCCUPANCY)).andExpect(status().isUnauthorized());
        mvc.perform(get(OCCUPANCY).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get(OCCUPANCY).with(user(OPERATOR).roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].capacity").value(50))
                .andExpect(jsonPath("$[0].occupied").value(0))
                .andExpect(jsonPath("$[1].occupied").value(1));
    }

    @Test
    void documentsTheContract() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + EXIT + "'].post.responses['503']").exists())
                .andExpect(jsonPath("$.paths['" + EXIT + "'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['" + OCCUPANCY + "'].get.responses['200']").exists())
                .andExpect(jsonPath("$.components.schemas.Occupancy.properties.occupied").exists())
                .andExpect(jsonPath("$.components.schemas.ExitResponse.properties.barrierOpen").exists())
                .andExpect(jsonPath("$.components.schemas.ExitResponse.properties.code.enum").isArray());
    }

    private void expectDecision(String plate, boolean open, String code) throws Exception {
        mvc.perform(asOperator(EXIT, plate)).andExpect(status().isOk())
                .andExpect(jsonPath("$.barrierOpen").value(open)).andExpect(jsonPath("$.code").value(code));
    }

    private void enter(String plate) throws Exception {
        mvc.perform(asOperator("/api/v1/entries/manual", plate)).andExpect(status().isCreated());
    }

    private void chargeCar() throws Exception {
        mvc.perform(asOperator("/api/v1/charges", "ABC123")).andExpect(status().is2xxSuccessful());
    }

    private void markPaid() {
        jdbc.sql("UPDATE cobros SET estado = 'PAGADO', vigente_hasta = generado_en").update();
    }

    private List<Long> consumedEntries() {
        return jdbc.sql("SELECT ingreso_id FROM eventos WHERE tipo_evento = 'SALIDA' ORDER BY id")
                .query(Long.class).list();
    }

    private int occupied(String type) {
        return jdbc.sql("SELECT ocupados FROM ocupacion WHERE sede_id = :site AND tipo_vehiculo = :type")
                .param("site", siteId).param("type", type).query(Integer.class).single();
    }

    private int occupancy() {
        return jdbc.sql("SELECT coalesce(sum(ocupados), 0) FROM ocupacion WHERE sede_id = :site")
                .param("site", siteId).query(Integer.class).single();
    }

    private MockHttpServletRequestBuilder asOperator(String path, String plate) {
        return request(path, plate).with(user(OPERATOR).roles("OPERADOR")).with(csrf());
    }

    private MockHttpServletRequestBuilder request(String path, String plate) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"" + plate + "\"}");
    }
}
