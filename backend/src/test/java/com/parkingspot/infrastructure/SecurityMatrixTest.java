package com.parkingspot.infrastructure;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkingspot.support.EntryIntegrationTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * HU-23 · CA-02 y HU-06 · CA-03/CA-04: matriz de autorización de toda la API con la SecurityConfig real.
 * Para cada endpoint protegido comprueba: sin sesión → 401, rol sin permiso → 403 y, en escrituras,
 * sin token CSRF → 403. Spring Security rechaza antes de llegar al controlador, así que corre sin Docker (H2).
 */
@EntryIntegrationTest
class SecurityMatrixTest {
    @Autowired MockMvc mvc;

    /** Endpoint, método y el rol que NO tiene permiso. */
    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/auth/me", null),
                Arguments.of(HttpMethod.POST, "/api/v1/vehicles/classify", "ADMIN"),
                Arguments.of(HttpMethod.POST, "/api/v1/plate-readings/simulate", "ADMIN"),
                Arguments.of(HttpMethod.POST, "/api/v1/entries/automatic", "ADMIN"),
                Arguments.of(HttpMethod.POST, "/api/v1/entries/manual", "ADMIN"),
                Arguments.of(HttpMethod.GET, "/api/v1/entries/latest", "ADMIN"),
                Arguments.of(HttpMethod.GET, "/api/v1/occupancy", "ADMIN"),
                Arguments.of(HttpMethod.POST, "/api/v1/exits/validate", "ADMIN"),
                Arguments.of(HttpMethod.POST, "/api/v1/charges", "ADMIN"),
                Arguments.of(HttpMethod.GET, "/api/v1/charges/today", "ADMIN"),
                Arguments.of(HttpMethod.POST, "/api/v1/payments", "ADMIN"),
                Arguments.of(HttpMethod.GET, "/api/v1/payments/charge/1", "ADMIN"),
                Arguments.of(HttpMethod.GET, "/api/v1/passes", "ADMIN"),
                Arguments.of(HttpMethod.GET, "/api/v1/history/events", null),
                Arguments.of(HttpMethod.GET, "/api/v1/history/charges", null),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/history", "OPERADOR"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/sites", "OPERADOR"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/sites/1/configuration", "OPERADOR"),
                Arguments.of(HttpMethod.PUT, "/api/v1/admin/sites/1/configuration", "OPERADOR"));
    }

    /** Solo las rutas que algún rol tiene prohibido (las de historial permiten OPERADOR y ADMIN). */
    static Stream<Arguments> roleRestrictedEndpoints() {
        return protectedEndpoints().filter(args -> args.get()[2] != null);
    }

    static Stream<Arguments> writeEndpoints() {
        return protectedEndpoints().filter(args -> args.get()[0] != HttpMethod.GET);
    }

    private static MockHttpServletRequestBuilder call(HttpMethod method, String path) {
        return request(method, path).contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    private static String allowedRole(String forbiddenRole) {
        return "ADMIN".equals(forbiddenRole) ? "OPERADOR" : "ADMIN";
    }

    @ParameterizedTest(name = "{0} {1} sin sesión → 401")
    @MethodSource("protectedEndpoints")
    @DisplayName("Sin sesión, toda ruta protegida responde 401 (aunque traiga CSRF)")
    void sinSesion401(HttpMethod method, String path, String forbiddenRole) throws Exception {
        mvc.perform(call(method, path).with(csrf())).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} con rol {2} → 403")
    @MethodSource("roleRestrictedEndpoints")
    @DisplayName("Con un rol sin permiso, la ruta responde 403")
    void rolSinPermiso403(HttpMethod method, String path, String forbiddenRole) throws Exception {
        mvc.perform(call(method, path).with(user("sin.permiso").roles(forbiddenRole)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1} sin CSRF → 403")
    @MethodSource("writeEndpoints")
    @DisplayName("Una escritura con el rol correcto pero sin token CSRF responde 403")
    void escrituraSinCsrf403(HttpMethod method, String path, String forbiddenRole) throws Exception {
        mvc.perform(call(method, path).with(user("con.permiso").roles(allowedRole(forbiddenRole))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1} no declarada")
    @MethodSource("undeclaredRoutes")
    @DisplayName("Una ruta no declarada se niega siempre (anyRequest().denyAll()): 401 sin sesión y 403 con sesión")
    void rutaNoDeclaradaSeNiega(HttpMethod method, String path) throws Exception {
        mvc.perform(call(method, path).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(call(method, path).with(user("operador").roles("OPERADOR")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(call(method, path).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    static Stream<Arguments> undeclaredRoutes() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/no-existe"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/entries/manual"),
                Arguments.of(HttpMethod.GET, "/api/v1/payments"),
                Arguments.of(HttpMethod.GET, "/actuator/env"));
    }
}
