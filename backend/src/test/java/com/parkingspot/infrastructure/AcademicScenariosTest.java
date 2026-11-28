package com.parkingspot.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HU-23 · CA-06: los ocho escenarios obligatorios de demostración (issue #37, HU-27) ejecutados de punta a punta
 * por la API REST, con la seguridad real (HU-06), las migraciones Flyway V1-V4 y un PostgreSQL 16 aislado
 * (Testcontainers). El escenario 8 es la ejecución de la suite completa, que se documenta en docs/testing.
 * Reloj fijo: 23/11/2026 a las 10:00 de Bogotá (franja DIURNA).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(ChargeApiTest.FixedClock.class)
class AcademicScenariosTest {
    private static final String OPERATOR_NAME = "operador.demo";
    private static final RequestPostProcessor OPERATOR = user(OPERATOR_NAME).roles("OPERADOR");
    private static final RequestPostProcessor ADMIN = user("admin.demo").roles("ADMIN");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper json;
    private long siteId;

    @BeforeEach
    void datosDeDemostracion() {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("DELETE FROM pagos").update();
        jdbc.sql("DELETE FROM cobros").update();
        jdbc.sql("DELETE FROM usuarios").update();
        jdbc.sql("UPDATE ocupacion SET ocupados = 0, capacidad = CASE tipo_vehiculo WHEN 'CARRO' THEN 50 ELSE 30 END")
                .update();
        jdbc.sql("""
                UPDATE tarifas SET valor = CASE WHEN tipo_vehiculo = 'MOTO' THEN 0
                                                WHEN franja = 'DIURNA' THEN 10000 ELSE 6000 END
                """).update();
        siteId = jdbc.sql("SELECT id FROM sedes WHERE nombre = 'Sede Principal'").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT :username, 'sin-uso', 'Operador de demostración', id, :site FROM roles WHERE nombre = 'OPERADOR'
                """).param("username", OPERATOR_NAME).param("site", siteId).update();
    }

    @Test
    @DisplayName("Escenario 1: una motocicleta ingresa y sale sin cobro")
    void escenario1MotoIngresaYSaleSinCobro() throws Exception {
        enter("ABC12D").andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleType").value("MOTO"))
                .andExpect(jsonPath("$.barreraAbierta").value(true));
        assertThat(chargesOf("ABC12D")).isZero();
        assertThat(occupied("MOTO")).isOne();

        exit("ABC12D").andExpect(status().isOk())
                .andExpect(jsonPath("$.barrierOpen").value(true))
                .andExpect(jsonPath("$.code").value("EXENCION_COMPROBADA"));
        assertThat(chargesOf("ABC12D")).isZero();
        assertThat(occupied("MOTO")).isZero();
    }

    @Test
    @DisplayName("Escenario 2: un carro que ingresa durante el día genera un cobro pendiente")
    void escenario2CarroDiurnoGeneraCobroPendiente() throws Exception {
        enter("ABC123").andExpect(status().isCreated()).andExpect(jsonPath("$.vehicleType").value("CARRO"));

        todayCharge("ABC123").andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlot").value("DIURNA"))
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.operatingDay").value("2026-11-23"));
        assertThat(chargesOf("ABC123")).isOne();
    }

    @Test
    @DisplayName("Escenario 3: un carro que intenta salir sin pagar encuentra la barrera cerrada")
    void escenario3SalidaSinPagoMantieneLaBarreraCerrada() throws Exception {
        enter("ABC123").andExpect(status().isCreated());

        exit("ABC123").andExpect(status().isOk())
                .andExpect(jsonPath("$.barrierOpen").value(false))
                .andExpect(jsonPath("$.code").value("PAGO_PENDIENTE"));
        assertThat(occupied("CARRO")).isOne();
        assertThat(jdbc.sql("""
                SELECT motivo FROM eventos WHERE placa = 'ABC123' AND tipo_evento = 'VALIDACION' AND resultado = 'RECHAZADO'
                """).query(String.class).single()).isEqualTo("PAGO_PENDIENTE");
    }

    @Test
    @DisplayName("Escenario 4: pago, salida y reingreso el mismo día sin un nuevo cobro")
    void escenario4PagoSalidaYReingresoSinNuevoCobro() throws Exception {
        enter("ABC123").andExpect(status().isCreated());
        long chargeId = chargeId("ABC123");

        mvc.perform(post("/api/v1/payments").with(OPERATOR).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeId\":%d,\"amount\":10000,\"idempotencyKey\":\"demo-4\"}".formatted(chargeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chargeStatus").value("PAGADO"))
                .andExpect(jsonPath("$.passValidUntil").exists());
        exit("ABC123").andExpect(status().isOk())
                .andExpect(jsonPath("$.barrierOpen").value(true))
                .andExpect(jsonPath("$.code").value("PAGO_CONFIRMADO"));
        assertThat(occupied("CARRO")).isZero();

        enter("ABC123").andExpect(status().isCreated());
        assertThat(chargesOf("ABC123")).isOne();
        todayCharge("ABC123").andExpect(jsonPath("$.chargeId").value(chargeId))
                .andExpect(jsonPath("$.status").value("PAGADO"));
    }

    @Test
    @DisplayName("Escenario 5: una lectura de baja confianza pide validación y el operador la confirma manualmente")
    void escenario5BajaConfianzaConValidacionManual() throws Exception {
        mvc.perform(post("/api/v1/entries/automatic").with(OPERATOR).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"DEF456\",\"confidence\":0.42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("REQUIERE_VALIDACION"))
                .andExpect(jsonPath("$.motivo").value("CONFIANZA_BAJA"))
                .andExpect(jsonPath("$.barreraAbierta").value(false));
        assertThat(eventsOf("DEF456")).isZero();
        assertThat(chargesOf("DEF456")).isZero();

        enter("DEF456").andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.operador").value(OPERATOR_NAME));
        var row = jdbc.sql("SELECT origen_lectura, confianza, usuario_id FROM eventos WHERE placa = 'DEF456'")
                .query().singleRow();
        assertThat(row.get("origen_lectura")).isEqualTo("MANUAL");
        assertThat(row.get("confianza")).isNull();
        assertThat(row.get("usuario_id")).isNotNull();
    }

    @Test
    @DisplayName("Escenario 6: el parqueadero lleno rechaza un nuevo ingreso")
    void escenario6ParqueaderoLlenoRechazaIngreso() throws Exception {
        jdbc.sql("UPDATE ocupacion SET capacidad = 1 WHERE tipo_vehiculo = 'CARRO'").update();
        enter("AAA111").andExpect(status().isCreated());

        enter("BBB222").andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("LLENO"))
                .andExpect(jsonPath("$.barreraAbierta").value(false));
        assertThat(occupied("CARRO")).isOne();
        assertThat(chargesOf("BBB222")).isZero();
        assertThat(jdbc.sql("SELECT resultado || '/' || motivo FROM eventos WHERE placa = 'BBB222'")
                .query(String.class).single()).isEqualTo("RECHAZADO/LLENO");
    }

    @Test
    @DisplayName("Escenario 7: se rechaza una tarifa nocturna mayor o igual a la diurna")
    void escenario7TarifaNocturnaMayorOIgualSeRechaza() throws Exception {
        String url = "/api/v1/admin/sites/" + siteId + "/configuration";
        var config = (ObjectNode) json.readTree(mvc.perform(get(url).with(ADMIN)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());

        for (int night : new int[] {10000, 12000}) {   // igual y mayor que la diurna de $10.000
            config.withObject("/car").put("day", 10000).put("night", night);
            mvc.perform(put(url).with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(config)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("CONFIGURACION_INVALIDA"));
        }
        assertThat(tariff("CARRO", "NOCTURNA")).isEqualByComparingTo("6000");
    }

    private ResultActions enter(String plate) throws Exception {
        return mvc.perform(post("/api/v1/entries/manual").with(OPERATOR).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"" + plate + "\"}"));
    }

    private ResultActions exit(String plate) throws Exception {
        return mvc.perform(post("/api/v1/exits/validate").with(OPERATOR).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"" + plate + "\"}"));
    }

    private ResultActions todayCharge(String plate) throws Exception {
        return mvc.perform(get("/api/v1/charges/today").with(OPERATOR).param("plate", plate));
    }

    private long chargeId(String plate) throws Exception {
        var body = todayCharge(plate).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        return json.readTree(body).path("chargeId").asLong();
    }

    private long chargesOf(String plate) {
        return jdbc.sql("SELECT COUNT(*) FROM cobros WHERE placa = :p").param("p", plate).query(Long.class).single();
    }

    private long eventsOf(String plate) {
        return jdbc.sql("SELECT COUNT(*) FROM eventos WHERE placa = :p").param("p", plate).query(Long.class).single();
    }

    private int occupied(String type) {
        return jdbc.sql("SELECT ocupados FROM ocupacion WHERE sede_id = :s AND tipo_vehiculo = :t")
                .param("s", siteId).param("t", type).query(Integer.class).single();
    }

    private BigDecimal tariff(String type, String slot) {
        return jdbc.sql("SELECT valor FROM tarifas WHERE sede_id = :s AND tipo_vehiculo = :t AND franja = :f")
                .param("s", siteId).param("t", type).param("f", slot).query(BigDecimal.class).single();
    }
}
