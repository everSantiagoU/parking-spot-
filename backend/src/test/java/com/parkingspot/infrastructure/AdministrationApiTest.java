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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(ChargeApiTest.FixedClock.class)
class AdministrationApiTest {
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final RequestPostProcessor OPERATOR = user("operador").roles("OPERADOR");
    private static final String HISTORY = "/api/v1/admin/history";

    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper json;
    private String configuration;

    @BeforeEach
    void reset() {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("DELETE FROM cobros").update();
        jdbc.sql("DELETE FROM usuarios").update();
        jdbc.sql("UPDATE ocupacion SET ocupados = 0").update();
        jdbc.sql("UPDATE tarifas SET valor = 10000 WHERE tipo_vehiculo = 'CARRO' AND franja = 'DIURNA'").update();
        long site = jdbc.sql("SELECT id FROM sedes WHERE nombre = 'Sede Principal'").query(Long.class).single();
        configuration = "/api/v1/admin/sites/" + site + "/configuration";
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT 'operador', 'sin-uso', 'Operador', id, :site FROM roles WHERE nombre = 'OPERADOR'
                """).param("site", site).update();
    }

    private void enter(String plate) throws Exception {
        mvc.perform(post("/api/v1/entries/manual").with(OPERATOR).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(java.util.Map.of("plate", plate))))
                .andExpect(status().isCreated());
    }

    @Test
    void onlyAdminCanSaveAndOnlyFutureChargesUseTheNewTariff() throws Exception {
        enter("ABC123");
        enter("ABC12D");
        var before = mvc.perform(get(configuration).with(ADMIN)).andExpect(status().isOk()).andReturn().getResponse();
        var config = json.readTree(before.getContentAsByteArray());
        var car = config.withObject("/car");
        car.put("day", 14000);
        var motorcycle = config.withObject("/motorcycle");
        motorcycle.put("capacity", 0);
        mvc.perform(put(configuration).with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(config))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La capacidad de MOTO es inferior a la ocupación actual."));
        motorcycle.put("capacity", 20);
        var save = put(configuration).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(config));
        mvc.perform(save).andExpect(status().isUnauthorized());
        mvc.perform(save.with(OPERATOR)).andExpect(status().isForbidden());
        var after = mvc.perform(get(configuration).with(ADMIN)).andReturn().getResponse();
        assertThat(json.readTree(after.getContentAsByteArray())).isEqualTo(json.readTree(before.getContentAsByteArray()));
        mvc.perform(save.with(ADMIN)).andExpect(status().isNoContent());
        mvc.perform(get(configuration).with(ADMIN)).andExpect(jsonPath("$.motorcycle.capacity").value(20))
                .andExpect(jsonPath("$.car.day").value(14000));
        enter("DEF456");
        mvc.perform(get("/api/v1/charges/today").with(OPERATOR).param("plate", "ABC123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.appliedTariff").value(10000));
        mvc.perform(get("/api/v1/charges/today").with(OPERATOR).param("plate", "DEF456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(14000))
                .andExpect(jsonPath("$.appliedTariff").value(14000));
    }

    @Test
    void historyCombinesInclusiveDatesPlateAndChargeStatusForAdministrators() throws Exception {
        enter("ABC123");
        enter("DEF456");
        var charges = get(HISTORY).param("kind", "COBROS").param("plate", " abc123 ")
                .param("from", "2026-11-23").param("to", "2026-11-23").param("status", "PENDIENTE");
        mvc.perform(charges).andExpect(status().isUnauthorized());
        mvc.perform(charges.with(OPERATOR)).andExpect(status().isForbidden());
        mvc.perform(charges.with(ADMIN)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].plate").value("ABC123")).andExpect(jsonPath("$[0].kind").value("COBRO"));
        mvc.perform(get(HISTORY).with(ADMIN).param("kind", "COBROS").param("plate", "ABC123").param("status", "PAGADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        var events = get(HISTORY).with(ADMIN).param("kind", "EVENTOS").param("plate", "DEF456");
        mvc.perform(events).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kind").value("INGRESO")).andExpect(jsonPath("$[0].plate").value("DEF456"));
        mvc.perform(events.param("from", "2026-11-24")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(HISTORY).with(ADMIN).param("kind", "COBROS").param("to", "2026-11-22"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
