package com.parkingspot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore;
import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:auth-schema.sql",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@Import(AuthenticationTest.TariffPolicyProbe.class)
class AuthenticationTest {
    private static final String AUTH = "/api/v1/auth";
    private static final String CLASSIFY = "/api/v1/vehicles/classify";
    private static final String TARIFF = "/api/v1/admin/tariffs/1";
    private final String password = UUID.randomUUID().toString();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountStore accounts;
    @Autowired JdbcClient jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clearAccounts() {
        jdbc.sql("DELETE FROM usuarios").update();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void authenticatesPersistedUsersAndReturnsOnlyUsernameAndRole(Role role) throws Exception {
        var session = signIn(role);
        mvc.perform(get(AUTH + "/me").session(session))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"username":"cuenta","role":"%s"}
                        """.formatted(role), JsonCompareMode.STRICT));
    }

    @Test
    void newAccountsStoreOnlyBcryptHashes() {
        accounts.create("cuenta", "Cuenta de prueba", Role.OPERADOR, password);
        var hash = jdbc.sql("SELECT password_hash FROM usuarios WHERE username = 'cuenta'")
                .query(String.class).single();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(hash).startsWith("$2").hasSize(60).isNotEqualTo(password);
        assertThat(encoder.matches(password, hash)).isTrue();
    }

    @Test
    void wrongPasswordUnknownUserAndDisabledAccountGetTheSameError() throws Exception {
        accounts.create("cuenta", "Cuenta de prueba", Role.OPERADOR, password);
        var expected = """
                {"code":"INVALID_CREDENTIALS","message":"Credenciales inválidas."}
                """;
        login("cuenta", UUID.randomUUID().toString(), csrf(null))
                .andExpect(status().isUnauthorized()).andExpect(content().json(expected, JsonCompareMode.STRICT));
        login("ausente", password, csrf(null))
                .andExpect(status().isUnauthorized()).andExpect(content().json(expected, JsonCompareMode.STRICT));
        jdbc.sql("UPDATE usuarios SET activo = FALSE WHERE username = 'cuenta'").update();
        login("cuenta", password, csrf(null))
                .andExpect(status().isUnauthorized()).andExpect(content().json(expected, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void onlyAdminMayReachTariffHandlerAndOnlyOperatorMayClassify(Role role) throws Exception {
        var token = csrf(signIn(role));
        mvc.perform(put(TARIFF).session(token.session()).header(token.header(), token.value()))
                .andExpect(status().is(role == Role.ADMIN ? 204 : 403));
        mvc.perform(post(CLASSIFY).session(token.session()).header(token.header(), token.value())
                        .contentType(APPLICATION_JSON).content("{\"plate\":\" abc123 \"}"))
                .andExpect(status().is(role == Role.OPERADOR ? 200 : 403));
    }

    @Test
    void protectedApiRequiresSessionEvenForPostWithoutCsrf() throws Exception {
        mvc.perform(get(AUTH + "/me")).andExpect(status().isUnauthorized());
        mvc.perform(post(CLASSIFY)).andExpect(status().isUnauthorized());
        mvc.perform(put(TARIFF).cookie(new Cookie("JSESSIONID", UUID.randomUUID().toString())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(AUTH + "/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void csrfIsRequiredForLoginAndAuthenticatedWrites() throws Exception {
        mvc.perform(post(AUTH + "/login")).andExpect(status().isForbidden());
        var session = signIn(Role.OPERADOR);
        mvc.perform(post(CLASSIFY).session(session)).andExpect(status().isForbidden());
        mvc.perform(post(AUTH + "/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get(AUTH + "/me").session(session)).andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesSessionAndReplayIsUnauthorized() throws Exception {
        var session = signIn(Role.OPERADOR);
        var token = csrf(session);
        var oldId = session.getId();
        mvc.perform(post(AUTH + "/logout").session(session).header(token.header(), token.value()))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge("JSESSIONID", 0));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get(AUTH + "/me").cookie(new Cookie("JSESSIONID", oldId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsCredentialedPreflightOnlyFromConfiguredFrontend() throws Exception {
        mvc.perform(options(CLASSIFY).header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options(CLASSIFY).header("Origin", "https://example.org")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession signIn(Role role) throws Exception {
        accounts.create("cuenta", "Cuenta de prueba", role, password);
        var token = csrf(null);
        var oldId = token.session().getId();
        var result = login("cuenta", password, token)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"username":"cuenta","role":"%s"}
                        """.formatted(role), JsonCompareMode.STRICT))
                .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(oldId);
        return session;
    }

    private ResultActions login(String username, String secret, Csrf token) throws Exception {
        return mvc.perform(post(AUTH + "/login").session(token.session()).header(token.header(), token.value())
                .contentType(APPLICATION_FORM_URLENCODED).param("username", username).param("password", secret));
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        var request = get(AUTH + "/csrf");
        if (session != null) {
            request.session(session);
        }
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Csrf((MockHttpSession) result.getRequest().getSession(false),
                body.get("headerName").asText(), body.get("token").asText());
    }

    private record Csrf(MockHttpSession session, String header, String value) {}

    @RestController
    static class TariffPolicyProbe {
        @PutMapping(TARIFF)
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void update() {}
    }
}
