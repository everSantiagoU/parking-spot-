package com.parkingspot.infrastructure;

import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore;
import com.parkingspot.support.EntryIntegrationTest;
import com.parkingspot.support.EntryTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-11: validación de la confianza y lectura manual, de punta a punta con la seguridad de la HU-06.
 * El operador existe en la base (usuarios.sede_id) y el umbral es 0.80.
 */
@EntryIntegrationTest
class EntryReadingControllerTest {
    private static final String AUTOMATIC = "/api/v1/entries/automatic";
    private static final String MANUAL = "/api/v1/entries/manual";

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

    @Test
    void acceptedAutomaticReadingContinuesToEntry() throws Exception {
        postAsOperator(AUTOMATIC, """
                {"plate":" abc123 ","confidence":0.93}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("AUTORIZADO"))
                .andExpect(jsonPath("$.plate").value("ABC123"))
                .andExpect(jsonPath("$.source").value("SIMULADOR"))
                .andExpect(jsonPath("$.operador").value(EntryTestData.OPERATOR));
    }

    // CA-02: frontera del umbral 0.80 — igual se acepta, menor pide validación.
    @Test
    void thresholdBoundary() throws Exception {
        postAsOperator(AUTOMATIC, "{\"plate\":\"ABC123\",\"confidence\":0.80}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("AUTORIZADO"));
        postAsOperator(AUTOMATIC, "{\"plate\":\"ABC123\",\"confidence\":0.79}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("REQUIERE_VALIDACION"))
                .andExpect(jsonPath("$.motivo").value("CONFIANZA_BAJA"))
                .andExpect(jsonPath("$.plate").value("ABC123"))
                .andExpect(jsonPath("$.barreraAbierta").value(false));
    }

    // Formato inválido → REQUIERE_VALIDACION aunque la confianza sea alta.
    @Test
    void invalidFormatRequiresValidation() throws Exception {
        postAsOperator(AUTOMATIC, "{\"plate\":\"AB123\",\"confidence\":0.99}")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"resultado":"REQUIERE_VALIDACION","motivo":"FORMATO_INVALIDO","barreraAbierta":false,
                         "eventoId":null,"sedeId":null,"ocurridoEn":null,"readText":"AB123","plate":null,
                         "vehicleType":null,"confidence":0.99,"source":"SIMULADOR","operador":"operador1"}""",
                        JsonCompareMode.STRICT));
    }

    // CA-04: sin la confirmación requerida no se registra la entrada (ni evento ni ocupación).
    @Test
    void noEntryIsRegisteredWithoutRequiredConfirmation() throws Exception {
        postAsOperator(AUTOMATIC, "{\"plate\":\"ABC123\",\"confidence\":0.42}")
                .andExpect(jsonPath("$.resultado").value("REQUIERE_VALIDACION"));
        postAsOperator(AUTOMATIC, "{\"plate\":\"AB123\",\"confidence\":0.99}")
                .andExpect(jsonPath("$.resultado").value("REQUIERE_VALIDACION"));

        assertThat(EntryTestData.events(jdbc)).isZero();
        assertThat(EntryTestData.occupied(jdbc, "CARRO")).isZero();

        // Con la confirmación del operador, la entrada sí se registra.
        postAsOperator(MANUAL, "{\"plate\":\"ABC123\"}").andExpect(status().isCreated());
        assertThat(EntryTestData.events(jdbc)).isEqualTo(1);
    }

    @Test
    void invalidConfidenceIsBadRequest() throws Exception {
        postAsOperator(AUTOMATIC, "{\"plate\":\"ABC123\",\"confidence\":1.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CONFIDENCE"));
    }

    // CA-03: la placa digitada o confirmada queda MANUAL, sin confianza y con el usuario.
    @Test
    void manualConfirmationIsMarkedManualWithOperator() throws Exception {
        postAsOperator(MANUAL, "{\"plate\":\" abc12d \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("AUTORIZADO"))
                .andExpect(jsonPath("$.readText").value(" abc12d "))
                .andExpect(jsonPath("$.plate").value("ABC12D"))
                .andExpect(jsonPath("$.vehicleType").value("MOTO"))
                .andExpect(jsonPath("$.confidence").isEmpty())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.operador").value(EntryTestData.OPERATOR));
    }

    @Test
    void manualConfirmationRejectsInvalidPlate() throws Exception {
        postAsOperator(MANUAL, "{\"plate\":\"AB123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLATE"));
        assertThat(EntryTestData.events(jdbc)).isZero();
    }

    @Test
    void inactiveOperatorIsForbidden() throws Exception {
        jdbc.sql("UPDATE usuarios SET activo = FALSE WHERE username = 'operador1'").update();
        postAsOperator(MANUAL, "{\"plate\":\"ABC123\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OPERATOR_NOT_AVAILABLE"));
    }

    // Seguridad (HU-06) en ambas rutas: sin sesión 401, ADMIN 403 y sin CSRF 403.
    @ParameterizedTest
    @ValueSource(strings = {AUTOMATIC, MANUAL})
    void enforcesOperatorRoleSessionAndCsrf(String url) throws Exception {
        var body = "{\"plate\":\"ABC123\",\"confidence\":0.9}";
        mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(post(url).with(user(EntryTestData.OPERATOR).roles("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertThat(EntryTestData.events(jdbc)).isZero();
    }
}
