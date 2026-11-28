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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
 * HU-12 (administrar tarifas, franjas y cupos) por la API con PostgreSQL aislado. La validación vive en
 * ParkingConfigurationStore y usa SQL de PostgreSQL (ON CONFLICT), por eso esta prueba necesita Testcontainers.
 * Reloj fijo: 23/11/2026 a las 10:00 de Bogotá.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(ChargeApiTest.FixedClock.class)
class ParkingConfigurationApiTest {
    private static final RequestPostProcessor ADMIN = user("admin.config").roles("ADMIN");
    private static final RequestPostProcessor OPERATOR = user("operador.config").roles("OPERADOR");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper json;
    private long siteId;
    private String url;

    @BeforeEach
    void configuracionInicial() {
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
        jdbc.sql("""
                UPDATE franjas SET hora_inicio = CASE nombre WHEN 'DIURNA' THEN TIME '06:00' ELSE TIME '18:00' END,
                                   hora_fin    = CASE nombre WHEN 'DIURNA' THEN TIME '18:00' ELSE TIME '06:00' END
                """).update();
        siteId = jdbc.sql("SELECT id FROM sedes WHERE nombre = 'Sede Principal'").query(Long.class).single();
        url = "/api/v1/admin/sites/" + siteId + "/configuration";
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT 'operador.config', 'sin-uso', 'Operador', id, :site FROM roles WHERE nombre = 'OPERADOR'
                """).param("site", siteId).update();
    }

    private ObjectNode current() throws Exception {
        return (ObjectNode) json.readTree(mvc.perform(get(url).with(ADMIN)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
    }

    private ResultActions save(ObjectNode config) throws Exception {
        return mvc.perform(put(url).with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(config)));
    }

    private BigDecimal tariff(String type, String slot) {
        return jdbc.sql("SELECT valor FROM tarifas WHERE sede_id = :s AND tipo_vehiculo = :t AND franja = :f")
                .param("s", siteId).param("t", type).param("f", slot).query(BigDecimal.class).single();
    }

    private int capacity(String type) {
        return jdbc.sql("SELECT capacidad FROM ocupacion WHERE sede_id = :s AND tipo_vehiculo = :t")
                .param("s", siteId).param("t", type).query(Integer.class).single();
    }

    @Test
    @DisplayName("La configuración inicial muestra franjas, tarifas y cupos de V2")
    void leeLaConfiguracionInicial() throws Exception {
        mvc.perform(get(url).with(ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.dayStart").value("06:00:00"))
                .andExpect(jsonPath("$.nightStart").value("18:00:00"))
                .andExpect(jsonPath("$.car.day").value(10000))
                .andExpect(jsonPath("$.car.night").value(6000))
                .andExpect(jsonPath("$.car.capacity").value(50))
                .andExpect(jsonPath("$.motorcycle.day").value(0))
                .andExpect(jsonPath("$.motorcycle.capacity").value(30));
        mvc.perform(get("/api/v1/admin/sites").with(ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sede Principal"));
    }

    @Test
    @DisplayName("CA-01: el administrador modifica tarifas por tipo de vehículo y franja")
    void modificaTarifasPorTipoYFranja() throws Exception {
        var config = current();
        config.withObject("/car").put("day", 12000).put("night", 7000);
        config.withObject("/motorcycle").put("day", 3000).put("night", 2000);

        save(config).andExpect(status().isNoContent());

        assertThat(tariff("CARRO", "DIURNA")).isEqualByComparingTo("12000");
        assertThat(tariff("CARRO", "NOCTURNA")).isEqualByComparingTo("7000");
        assertThat(tariff("MOTO", "DIURNA")).isEqualByComparingTo("3000");
        assertThat(tariff("MOTO", "NOCTURNA")).isEqualByComparingTo("2000");
    }

    @ParameterizedTest(name = "{0}: diurna {1}, nocturna {2} → 422")
    @CsvSource({
            "car, 10000, 10000",
            "car, 10000, 12000",
            "car, 10000, 0",
            "motorcycle, 2000, 2000",
            "motorcycle, 2000, 3000"
    })
    @DisplayName("CA-02: la tarifa nocturna debe ser menor que la diurna; si no, 422 y nada cambia")
    void rechazaNocturnaMayorOIgual(String vehicle, int day, int night) throws Exception {
        var config = current();
        config.withObject("/" + vehicle).put("day", day).put("night", night);

        save(config).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONFIGURACION_INVALIDA"))
                .andExpect(jsonPath("$.message").value(
                        "La tarifa nocturna debe ser menor que la diurna. El carro requiere tarifas positivas."));
        assertThat(tariff("CARRO", "DIURNA")).isEqualByComparingTo("10000");
        assertThat(tariff("CARRO", "NOCTURNA")).isEqualByComparingTo("6000");
        assertThat(tariff("MOTO", "DIURNA")).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("CA-02 (RN-01): la moto exenta con $0 en ambas franjas sí es válida")
    void motoExentaEsValida() throws Exception {
        var config = current();
        config.withObject("/motorcycle").put("day", 0).put("night", 0);
        save(config).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("CA-03: las franjas tienen horarios configurables y el cobro usa la nueva franja")
    void franjasConfigurables() throws Exception {
        var config = current();
        config.put("dayStart", "11:00:00").put("nightStart", "23:00:00");

        save(config).andExpect(status().isNoContent());

        mvc.perform(get(url).with(ADMIN))
                .andExpect(jsonPath("$.dayStart").value("11:00:00"))
                .andExpect(jsonPath("$.nightStart").value("23:00:00"));
        // El reloj marca las 10:00: con la diurna desde las 11:00, ahora es franja NOCTURNA ($6.000).
        mvc.perform(post("/api/v1/charges").with(OPERATOR).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"ABC123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timeSlot").value("NOCTURNA"))
                .andExpect(jsonPath("$.amount").value(6000));
    }

    @Test
    @DisplayName("CA-03: dos franjas que empiezan a la misma hora se rechazan")
    void franjasIgualesSeRechazan() throws Exception {
        var config = current();
        config.put("dayStart", "06:00:00").put("nightStart", "06:00:00");

        save(config).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Las franjas deben empezar a horas distintas."));
    }

    @ParameterizedTest(name = "capacidad {0} → 422")
    @CsvSource({"-1", "-50"})
    @DisplayName("CA-04: los cupos no aceptan valores negativos")
    void cuposNegativosSeRechazan(int value) throws Exception {
        var config = current();
        config.withObject("/car").put("capacity", value);

        save(config).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONFIGURACION_INVALIDA"));
        assertThat(capacity("CARRO")).isEqualTo(50);
    }

    @Test
    @DisplayName("CA-04: un cupo en cero es válido si no hay vehículos dentro")
    void cupoEnCeroEsValido() throws Exception {
        var config = current();
        config.withObject("/car").put("capacity", 0);
        save(config).andExpect(status().isNoContent());
        assertThat(capacity("CARRO")).isZero();
    }

    @Test
    @DisplayName("Importes negativos o con más de dos decimales se rechazan")
    void importesInvalidos() throws Exception {
        var config = current();
        config.withObject("/car").put("day", -10000);
        save(config).andExpect(status().isUnprocessableEntity());

        config.withObject("/car").put("day", new BigDecimal("10000.555"));
        save(config).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Consistencia transaccional: si la moto es inválida, el cambio válido del carro tampoco se guarda")
    void guardarEsAtomico() throws Exception {
        var config = current();
        config.withObject("/car").put("day", 15000).put("night", 9000);
        config.withObject("/motorcycle").put("capacity", -1);

        save(config).andExpect(status().isUnprocessableEntity());

        assertThat(tariff("CARRO", "DIURNA")).isEqualByComparingTo("10000");
        assertThat(tariff("CARRO", "NOCTURNA")).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("CA-05: el operador no puede consultar ni editar la configuración; sin sesión 401; sin CSRF 403")
    void soloElAdministrador() throws Exception {
        var body = json.writeValueAsBytes(current());

        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).with(OPERATOR)).andExpect(status().isForbidden());
        mvc.perform(put(url).with(OPERATOR).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put(url).with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertThat(tariff("CARRO", "DIURNA")).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("Una sede inexistente responde 404")
    void sedeInexistente() throws Exception {
        mvc.perform(get("/api/v1/admin/sites/999999/configuration").with(ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sede no disponible."));
    }
}
