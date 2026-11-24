package com.parkingspot.infrastructure;

import com.parkingspot.support.EntryIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Evidencia de que los endpoints del flujo de ingreso quedan documentados en OpenAPI/Swagger. */
@EntryIntegrationTest
class EntryApiDocsTest {
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/entries/automatic", "/api/v1/entries/manual"})
    void documentsEntryEndpoints(String path) throws Exception {
        var operation = "$.paths['" + path + "'].post";
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(operation + ".summary").isNotEmpty())
                .andExpect(jsonPath(operation + ".requestBody.required").value(true))
                .andExpect(jsonPath(operation + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/EntryResponse"))
                .andExpect(jsonPath(operation + ".responses['400']").exists())
                .andExpect(jsonPath(operation + ".responses['401']").exists())
                .andExpect(jsonPath(operation + ".responses['403'].content['application/json'].examples.ACCESS_DENIED")
                        .exists());
    }
}
