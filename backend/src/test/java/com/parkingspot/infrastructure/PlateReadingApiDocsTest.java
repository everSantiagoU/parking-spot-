package com.parkingspot.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evidencia de que el endpoint del simulador queda documentado en OpenAPI/Swagger:
 * método, cuerpo, respuestas 200 y 400 y ejemplos de cada código de error.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlateReadingApiDocsTest {
    private static final String OPERATION = "$.paths['/api/v1/plate-readings/simulate'].post";
    private static final String ERROR_EXAMPLES = OPERATION + ".responses['400'].content['application/json'].examples";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsSimulatorEndpointInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(OPERATION + ".summary").value("Simula la lectura de una placa"))
                .andExpect(jsonPath(OPERATION + ".requestBody.required").value(true))
                .andExpect(jsonPath(OPERATION + ".requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/SimulatedReadingRequest"))
                .andExpect(jsonPath(OPERATION + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/PlateReadingResponse"))
                .andExpect(jsonPath(OPERATION + ".responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath(ERROR_EXAMPLES + ".INVALID_CONFIDENCE.value.code").value("INVALID_CONFIDENCE"))
                .andExpect(jsonPath(ERROR_EXAMPLES + ".INVALID_PLATE.value.code").value("INVALID_PLATE"))
                .andExpect(jsonPath(ERROR_EXAMPLES + ".INVALID_REQUEST.value.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.components.schemas.SimulatedReadingRequest.properties.confidence.minimum").value(0))
                .andExpect(jsonPath("$.components.schemas.SimulatedReadingRequest.properties.confidence.maximum").value(1))
                .andExpect(jsonPath("$.components.schemas.SimulatedReadingRequest.required").value(hasItem("confidence")))
                .andExpect(jsonPath("$.components.schemas.PlateReadingResponse.properties.source.enum")
                        .value(hasItem("SIMULADOR")));
    }
}
