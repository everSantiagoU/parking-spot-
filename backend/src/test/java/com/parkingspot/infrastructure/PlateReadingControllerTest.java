package com.parkingspot.infrastructure;

import com.parkingspot.infrastructure.adapter.in.rest.PlateReadingController;
import com.parkingspot.infrastructure.config.PlateReaderConfig;
import com.parkingspot.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del endpoint del simulador. Sin mocks: corren el controlador real,
 * PlateReadingService, SimulatedPlateReader, PlateClassifier, los @RestControllerAdvice
 * (PlateReadingErrorHandler y ClassificationErrorHandler) y la política real de SecurityConfig (HU-06),
 * solo con la capa web y sin base de datos.
 */
@WebMvcTest(PlateReadingController.class)
@Import({PlateReaderConfig.class, SecurityConfig.class})
class PlateReadingControllerTest {
    private static final String URL = "/api/v1/plate-readings/simulate";
    private static final String VALID_BODY = """
            {"plate":"ABC123","confidence":0.9}""";
    private static final String INVALID_CONFIDENCE = """
            {"code":"INVALID_CONFIDENCE","message":"La confianza debe ser un número entre 0 y 1."}""";
    private static final String INVALID_PLATE = """
            {"code":"INVALID_PLATE","message":"La placa debe tener el formato ABC123 (carro) o ABC12D (moto)."}""";
    private static final String INVALID_REQUEST = """
            {"code":"INVALID_REQUEST","message":"La solicitud debe contener un JSON válido."}""";
    private static final String ACCESS_DENIED = """
            {"code":"ACCESS_DENIED","message":"Acceso denegado."}""";

    @Autowired
    private MockMvc mockMvc;

    /** Petición de un operador autenticado y con token CSRF, como la haría el panel de carril. */
    private ResultActions postAsOperator(String body) throws Exception {
        return mockMvc.perform(post(URL)
                .with(user("operador").roles("OPERADOR"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // CA-01 y CA-04: recibe placa y confianza, y responde placa y fuente (reutiliza la HU-09).
    @Test
    void returnsNormalizedPlateTypeConfidenceAndSimulatorSource() throws Exception {
        postAsOperator("""
                {"plate":" abc12d ","confidence":0.93}""")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"plate":"ABC12D","vehicleType":"MOTO","confidence":0.93,"source":"SIMULADOR"}""",
                        JsonCompareMode.STRICT));
    }

    // La HU-08 no aplica umbral (es de la HU-11): una confianza baja con placa válida responde 200.
    @Test
    void acceptsLowConfidenceBecauseThresholdBelongsToHu11() throws Exception {
        postAsOperator("""
                {"plate":"ABC123","confidence":0.2}""")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"plate":"ABC123","vehicleType":"CARRO","confidence":0.2,"source":"SIMULADOR"}""",
                        JsonCompareMode.STRICT));
    }

    // CA-02: los límites 0 y 1 son válidos.
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.0})
    void acceptsConfidenceBoundaries(double confidence) throws Exception {
        postAsOperator("{\"plate\":\"ABC123\",\"confidence\":" + confidence + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence").value(confidence))
                .andExpect(jsonPath("$.source").value("SIMULADOR"));
    }

    // CA-02: fuera de [0, 1], ausente o nula → 400 INVALID_CONFIDENCE.
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"plate\":\"ABC123\",\"confidence\":1.5}",
            "{\"plate\":\"ABC123\",\"confidence\":-0.1}",
            "{\"plate\":\"ABC123\",\"confidence\":null}",
            "{\"plate\":\"ABC123\"}"
    })
    void rejectsInvalidConfidence(String body) throws Exception {
        postAsOperator(body)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(INVALID_CONFIDENCE, JsonCompareMode.STRICT));
    }

    // INVALID_PLATE lo responde el ClassificationErrorHandler existente, con el mismo formato {code, message}.
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"plate\":\"AB123\",\"confidence\":0.9}",
            "{\"confidence\":0.9}"
    })
    void rejectsInvalidPlateWithSameErrorFormat(String body) throws Exception {
        postAsOperator(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(content().json(INVALID_PLATE, JsonCompareMode.STRICT));
    }

    // INVALID_REQUEST también sale del ClassificationErrorHandler existente, con el mismo formato.
    @ParameterizedTest
    @ValueSource(strings = {"{malo", "null", "", "{\"plate\":\"ABC123\",\"confidence\":\"alta\"}"})
    void rejectsMalformedRequestWithSameErrorFormat(String body) throws Exception {
        postAsOperator(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(content().json(INVALID_REQUEST, JsonCompareMode.STRICT));
    }

    // Seguridad (HU-06): sin sesión → 401, aunque la petición traiga CSRF.
    @Test
    void rejectsRequestWithoutSession() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // Seguridad (HU-06): el administrador no puede usar el simulador → 403.
    @Test
    void rejectsAdminRole() throws Exception {
        mockMvc.perform(post(URL)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().json(ACCESS_DENIED, JsonCompareMode.STRICT));
    }

    // Seguridad (HU-06): un operador con sesión pero sin token CSRF → 403.
    @Test
    void rejectsOperatorWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(URL)
                        .with(user("operador").roles("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().json(ACCESS_DENIED, JsonCompareMode.STRICT));
    }
}
