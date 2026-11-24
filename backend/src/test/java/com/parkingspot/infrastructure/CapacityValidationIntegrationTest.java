package com.parkingspot.infrastructure;

import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore;
import com.parkingspot.support.EntryIntegrationTest;
import com.parkingspot.support.EntryTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-13: validar el cupo antes de abrir la entrada, de punta a punta con H2 en modo PostgreSQL. */
@EntryIntegrationTest
class CapacityValidationIntegrationTest {
    private static final String AUTOMATIC = "/api/v1/entries/automatic";
    private static final String MANUAL = "/api/v1/entries/manual";

    @Autowired MockMvc mvc;
    @Autowired AccountStore accounts;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        EntryTestData.reset(jdbc, accounts);
    }

    private void setCapacity(String vehicleType, int capacity) {
        jdbc.sql("UPDATE ocupacion SET capacidad = :c WHERE tipo_vehiculo = :t")
                .param("c", capacity).param("t", vehicleType).update();
    }

    private ResultActions postAsOperator(String url, String body) throws Exception {
        return mvc.perform(post(url).with(user(EntryTestData.OPERATOR).roles("OPERADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // CA-02 y CA-03: sin cupo, la respuesta es LLENO y la barrera queda cerrada.
    @Test
    void fullSiteAnswersLlenoAndKeepsBarrierClosed() throws Exception {
        setCapacity("CARRO", 1);
        postAsOperator(AUTOMATIC, "{\"plate\":\"AAA111\",\"confidence\":0.95}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barreraAbierta").value(true));

        postAsOperator(AUTOMATIC, "{\"plate\":\"BBB222\",\"confidence\":0.95}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("LLENO"))
                .andExpect(jsonPath("$.motivo").value("LLENO"))
                .andExpect(jsonPath("$.barreraAbierta").value(false))
                .andExpect(jsonPath("$.eventoId").isNumber())
                .andExpect(jsonPath("$.plate").value("BBB222"));

        assertThat(EntryTestData.occupied(jdbc, "CARRO")).isEqualTo(1);
        // El intento rechazado queda en eventos para la trazabilidad.
        var rejected = jdbc.sql("SELECT * FROM eventos WHERE placa = 'BBB222'").query().singleRow();
        assertThat(rejected.get("RESULTADO")).isEqualTo("RECHAZADO");
        assertThat(rejected.get("MOTIVO")).isEqualTo("LLENO");
        assertThat(rejected.get("TIPO_EVENTO")).isEqualTo("INGRESO");
        assertThat(EntryTestData.events(jdbc)).isEqualTo(2);
    }

    // CA-01: el cupo es por tipo de vehículo; los carros llenos no bloquean a las motos.
    @Test
    void capacityIsValidatedPerVehicleType() throws Exception {
        setCapacity("CARRO", 0);

        postAsOperator(AUTOMATIC, "{\"plate\":\"ABC123\",\"confidence\":0.95}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("LLENO"));
        postAsOperator(AUTOMATIC, "{\"plate\":\"ABC12D\",\"confidence\":0.95}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("AUTORIZADO"))
                .andExpect(jsonPath("$.vehicleType").value("MOTO"));

        assertThat(EntryTestData.occupied(jdbc, "CARRO")).isZero();
        assertThat(EntryTestData.occupied(jdbc, "MOTO")).isEqualTo(1);
    }

    // La lectura manual también respeta el cupo.
    @Test
    void manualEntryAlsoRespectsCapacity() throws Exception {
        setCapacity("MOTO", 0);

        postAsOperator(MANUAL, "{\"plate\":\"ABC12D\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("LLENO"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.barreraAbierta").value(false));
        var rejected = jdbc.sql("SELECT * FROM eventos").query().singleRow();
        assertThat(rejected.get("ORIGEN_LECTURA")).isEqualTo("MANUAL");
        assertThat(rejected.get("RESULTADO")).isEqualTo("RECHAZADO");
    }

    // Un vehículo que libera cupo (por ejemplo, una salida futura) vuelve a permitir el ingreso.
    @Test
    void freedSpotAllowsEntryAgain() throws Exception {
        setCapacity("CARRO", 1);
        postAsOperator(MANUAL, "{\"plate\":\"AAA111\"}").andExpect(status().isCreated());
        postAsOperator(MANUAL, "{\"plate\":\"BBB222\"}").andExpect(status().isConflict());

        jdbc.sql("UPDATE ocupacion SET ocupados = ocupados - 1 WHERE tipo_vehiculo = 'CARRO'").update();

        postAsOperator(MANUAL, "{\"plate\":\"BBB222\"}").andExpect(status().isCreated());
    }
}
