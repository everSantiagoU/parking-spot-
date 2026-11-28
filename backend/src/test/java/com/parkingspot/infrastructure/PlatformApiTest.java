package com.parkingspot.infrastructure;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkingspot.support.EntryIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HU-02 (salud), HU-28 (OpenAPI/Swagger) y HU-09 (clasificación por la API), sin Docker (H2).
 */
@EntryIntegrationTest
class PlatformApiTest {
    @Autowired MockMvc mvc;

    @Test
    @DisplayName("HU-02 CA-02: /actuator/health es público y responde UP con la base de datos")
    void healthEsPublicoYUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    @DisplayName("HU-28 CA-02 y CA-06: Swagger UI y la especificación OpenAPI son públicos")
    void swaggerYOpenApiPublicos() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Parking Spot API"));
    }

    @ParameterizedTest(name = "{0} {1} está documentado")
    @CsvSource({
            "post, /api/v1/vehicles/classify",
            "post, /api/v1/plate-readings/simulate",
            "post, /api/v1/entries/automatic",
            "post, /api/v1/entries/manual",
            "get,  /api/v1/entries/latest",
            "get,  /api/v1/occupancy",
            "post, /api/v1/exits/validate",
            "post, /api/v1/charges",
            "get,  /api/v1/charges/today",
            "post, /api/v1/payments",
            "get,  /api/v1/payments/charge/{chargeId}",
            "get,  /api/v1/passes",
            "get,  /api/v1/history/events",
            "get,  /api/v1/history/charges",
            "get,  /api/v1/admin/history",
            "get,  /api/v1/admin/sites",
            "get,  /api/v1/admin/sites/{site}/configuration",
            "put,  /api/v1/admin/sites/{site}/configuration",
            "get,  /api/v1/auth/me",
            "get,  /api/v1/auth/csrf"
    })
    @DisplayName("HU-28 CA-03: cada endpoint REST aparece en la especificación OpenAPI con sus respuestas")
    void cadaEndpointEstaDocumentado(String method, String path) throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses").isNotEmpty());
    }

    @Test
    @DisplayName("HU-09 CA-01 y CA-03: la API normaliza y clasifica un carro")
    void clasificaCarro() throws Exception {
        classify(" abc123 ").andExpect(status().isOk())
                .andExpect(content().json("{\"plate\":\"ABC123\",\"type\":\"CARRO\"}", JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("HU-09 CA-02: la API clasifica una moto")
    void clasificaMoto() throws Exception {
        classify("abc12d").andExpect(status().isOk()).andExpect(jsonPath("$.type").value("MOTO"));
    }

    @ParameterizedTest(name = "placa \"{0}\" → 400 INVALID_PLATE")
    @CsvSource({"AB123", "ABC1234", "ABC 123", "''"})
    @DisplayName("HU-09 CA-04: un formato inválido responde 400 INVALID_PLATE")
    void formatoInvalido(String plate) throws Exception {
        classify(plate).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PLATE"));
    }

    private org.springframework.test.web.servlet.ResultActions classify(String plate) throws Exception {
        return mvc.perform(post("/api/v1/vehicles/classify").with(user("operador").roles("OPERADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"" + plate + "\"}"));
    }
}
