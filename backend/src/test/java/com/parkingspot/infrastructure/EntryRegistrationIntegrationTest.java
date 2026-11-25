package com.parkingspot.infrastructure;

import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore;
import com.parkingspot.support.EntryIntegrationTest;
import com.parkingspot.support.EntryTestData;
import com.parkingspot.support.FixedClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-10: el ingreso queda guardado en eventos y actualiza ocupacion (H2 en modo PostgreSQL). */
@EntryIntegrationTest
class EntryRegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AccountStore accounts;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        EntryTestData.reset(jdbc, accounts);
    }

    private ResultActions postAsOperator(String url, String body) throws Exception {
        return mvc.perform(post(url).with(user(EntryTestData.OPERATOR).roles("OPERADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Map<String, Object> event(long id) {
        return jdbc.sql("SELECT * FROM eventos WHERE id = :id").param("id", id).query().singleRow();
    }

    // CA-01, CA-02, CA-03 y CA-04 con una lectura automática.
    @Test
    void automaticEntryIsStoredWithSiteServerTimeAndSourceAndUpdatesOccupancy() throws Exception {
        var response = postAsOperator("/api/v1/entries/automatic", """
                {"plate":" abc123 ","confidence":0.93}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("AUTORIZADO"))
                .andExpect(jsonPath("$.barreraAbierta").value(true))
                .andExpect(jsonPath("$.sedeId").value(EntryTestData.siteId(jdbc)))
                .andExpect(jsonPath("$.ocurridoEn").value("2026-11-24T18:45:00Z"))
                .andReturn();
        long eventId = Long.parseLong(
                com.jayway.jsonpath.JsonPath.read(response.getResponse().getContentAsString(), "$.eventoId").toString());

        var row = event(eventId);
        assertThat(row.get("PLACA")).isEqualTo("ABC123");                                   // CA-01
        assertThat(row.get("SEDE_ID")).isEqualTo(EntryTestData.siteId(jdbc));              // CA-01
        assertThat(row.get("TIPO_VEHICULO")).isEqualTo("CARRO");
        assertThat(row.get("TIPO_EVENTO")).isEqualTo("INGRESO");
        assertThat(((OffsetDateTime) row.get("OCURRIDO_EN")).toInstant()).isEqualTo(FixedClockConfig.NOW); // CA-02
        assertThat(row.get("ORIGEN_LECTURA")).isEqualTo("AUTOMATICA");                      // CA-04
        assertThat((BigDecimal) row.get("CONFIANZA")).isEqualByComparingTo("93.00");        // 0.93 × 100
        assertThat(row.get("RESULTADO")).isEqualTo("AUTORIZADO");
        assertThat(row.get("MOTIVO")).isNull();
        assertThat(row.get("USUARIO_ID")).isEqualTo(EntryTestData.operatorId(jdbc));
        assertThat(EntryTestData.occupied(jdbc, "CARRO")).isEqualTo(1);                    // CA-03
        assertThat(EntryTestData.occupied(jdbc, "MOTO")).isZero();
    }

    // CA-04: una lectura manual queda MANUAL, sin confianza y con el usuario (exigido por la base).
    @Test
    void manualEntryIsStoredAsManualWithUserAndNoConfidence() throws Exception {
        postAsOperator("/api/v1/entries/manual", "{\"plate\":\"abc12d\"}")
                .andExpect(status().isCreated());

        var row = jdbc.sql("SELECT * FROM eventos").query().singleRow();
        assertThat(row.get("PLACA")).isEqualTo("ABC12D");
        assertThat(row.get("TIPO_VEHICULO")).isEqualTo("MOTO");
        assertThat(row.get("ORIGEN_LECTURA")).isEqualTo("MANUAL");
        assertThat(row.get("CONFIANZA")).isNull();
        assertThat(row.get("USUARIO_ID")).isEqualTo(EntryTestData.operatorId(jdbc));
        assertThat(EntryTestData.occupied(jdbc, "MOTO")).isEqualTo(1);
    }

    // CA-02: la petición no puede imponer la hora; un campo extra se ignora y manda el reloj del servidor.
    @Test
    void ignoresClientProvidedTime() throws Exception {
        postAsOperator("/api/v1/entries/manual", """
                {"plate":"ABC123","ocurridoEn":"2020-01-01T00:00:00Z"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocurridoEn").value("2026-11-24T18:45:00Z"));
    }

    // Transacción: si falla el INSERT del evento después de sumar la ocupación, se revierte todo.
    @Test
    void occupancyIsRolledBackWhenEventCannotBeStored() {
        jdbc.sql("ALTER TABLE eventos ADD CONSTRAINT ck_prueba_rollback CHECK (placa <> 'ZZZ999')").update();
        try {
            assertThatThrownBy(() -> postAsOperator("/api/v1/entries/manual", "{\"plate\":\"ZZZ999\"}"))
                    .hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertThat(EntryTestData.occupied(jdbc, "CARRO")).isZero();
            assertThat(EntryTestData.events(jdbc)).isZero();
        } finally {
            jdbc.sql("ALTER TABLE eventos DROP CONSTRAINT ck_prueba_rollback").update();
        }
    }

    // Si no hay cupos configurados, no queda ni evento ni cambio de ocupación.
    @Test
    void nothingIsStoredWhenOccupancyIsNotConfigured() throws Exception {
        jdbc.sql("DELETE FROM ocupacion WHERE tipo_vehiculo = 'MOTO'").update();
        try {
            postAsOperator("/api/v1/entries/manual", "{\"plate\":\"ABC12D\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("OCCUPANCY_NOT_CONFIGURED"));
            assertThat(EntryTestData.events(jdbc)).isZero();
        } finally {
            jdbc.sql("INSERT INTO ocupacion (sede_id, tipo_vehiculo, capacidad) "
                    + "SELECT id, 'MOTO', 30 FROM sedes WHERE nombre = 'Sede Principal'").update();
        }
    }
}
