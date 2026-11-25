package com.parkingspot.infrastructure;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HU-20 de punta a punta con PostgreSQL real. Datos: dos sedes; ABC123 tiene cobros en ambas,
 * XYZ789 solo en la Sede Principal, que es la del operador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class HistoryApiTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String OPERATOR = "operador.historial";
    private static final String MAIN_SITE = "Sede Principal";
    private static final String OTHER_SITE = "Sede Norte";
    private static final String EVENTS = "/api/v1/history/events";
    private static final String CHARGES = "/api/v1/history/charges";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void datos() {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("DELETE FROM pagos").update();
        jdbc.sql("DELETE FROM cobros").update();
        jdbc.sql("DELETE FROM usuarios").update();
        jdbc.sql("DELETE FROM sedes WHERE nombre = :name").param("name", OTHER_SITE).update();
        jdbc.sql("INSERT INTO sedes (nombre) VALUES (:name)").param("name", OTHER_SITE).update();
        jdbc.sql("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                SELECT :username, 'sin-uso', 'Operador de historial', r.id, s.id
                FROM roles r, sedes s WHERE r.nombre = 'OPERADOR' AND s.nombre = :site
                """).param("username", OPERATOR).param("site", MAIN_SITE).update();

        charge(MAIN_SITE, "ABC123", LocalDate.of(2026, 11, 22), "PAGADO");
        charge(MAIN_SITE, "ABC123", LocalDate.of(2026, 11, 23), "PENDIENTE");
        charge(MAIN_SITE, "XYZ789", LocalDate.of(2026, 11, 23), "PAGADO");
        charge(OTHER_SITE, "ABC123", LocalDate.of(2026, 11, 23), "PENDIENTE");

        event(MAIN_SITE, "ABC123", "INGRESO", LocalDateTime.of(2026, 11, 23, 8, 0));
        event(MAIN_SITE, "ABC123", "SALIDA", LocalDateTime.of(2026, 11, 23, 18, 0));
        event(MAIN_SITE, "XYZ789", "INGRESO", LocalDateTime.of(2026, 11, 24, 7, 0));
        event(OTHER_SITE, "ABC123", "INGRESO", LocalDateTime.of(2026, 11, 23, 9, 0));
    }

    @Test
    @DisplayName("CA-01: el operador ve los ingresos y salidas de su sede, del más reciente al más antiguo")
    void listadoDeEventos() throws Exception {
        asOperator(get(EVENTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[0].plate").value("XYZ789"))
                .andExpect(jsonPath("$.items[0].eventType").value("INGRESO"))
                .andExpect(jsonPath("$.items[1].eventType").value("SALIDA"));
    }

    @Test
    @DisplayName("CA-02: el historial de cobros muestra importe, día, placa y estado")
    void listadoDeCobros() throws Exception {
        asOperator(get(CHARGES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[0].amount").value(10000))
                .andExpect(jsonPath("$.items[0].operatingDay").value("2026-11-23"))
                .andExpect(jsonPath("$.items[0].plate").exists())
                .andExpect(jsonPath("$.items[0].status").exists());
    }

    @Test
    @DisplayName("CP-HU-20-E01 / CA-03: el filtro por placa devuelve solo sus registros; el administrador ve todas las sedes")
    void filtroPorPlaca() throws Exception {
        asOperator(get(EVENTS).param("plate", "abc123"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].plate").value("ABC123"))
                .andExpect(jsonPath("$.items[1].plate").value("ABC123"));
        asOperator(get(CHARGES).param("plate", "ABC123"))
                .andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(get(CHARGES).param("plate", "ABC123").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    @DisplayName("CP-HU-20-E02 / CA-04 / CA-05: fecha y estado combinados")
    void filtroPorFechaYEstado() throws Exception {
        asOperator(get(CHARGES).param("from", "2026-11-23").param("to", "2026-11-23").param("status", "PAGADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].plate").value("XYZ789"))
                .andExpect(jsonPath("$.items[0].status").value("PAGADO"));
        asOperator(get(EVENTS).param("from", "2026-11-23").param("to", "2026-11-23"))
                .andExpect(jsonPath("$.totalItems").value(2));
        asOperator(get(EVENTS).param("type", "SALIDA"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @DisplayName("CA-06: sin coincidencias responde 200 con la lista vacía")
    void consultaSinResultados() throws Exception {
        asOperator(get(CHARGES).param("plate", "DEF456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("Paginación básica: tamaño 1, segunda página")
    void paginacion() throws Exception {
        asOperator(get(EVENTS).param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("CP-HU-20-X01: intervalo invertido, estado inexistente o fecha mal escrita responden 400")
    void filtrosInvalidos() throws Exception {
        asOperator(get(EVENTS).param("from", "2026-11-25").param("to", "2026-11-23"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILTRO_INVALIDO"));
        asOperator(get(CHARGES).param("status", "REGALADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILTRO_INVALIDO"));
        asOperator(get(CHARGES).param("from", "23/11/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-HU-20-X02 / CA-07: sin sesión 401 y con un rol sin permiso 403")
    void controlDeAcceso() throws Exception {
        mvc.perform(get(EVENTS)).andExpect(status().isUnauthorized());
        mvc.perform(get(CHARGES).with(user("invitado").roles("INVITADO"))).andExpect(status().isForbidden());
    }

    private ResultActions asOperator(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(user(OPERATOR).roles("OPERADOR")));
    }

    private void charge(String site, String plate, LocalDate day, String state) {
        OffsetDateTime validUntil = "PAGADO".equals(state)
                ? day.plusDays(1).atTime(6, 0).atZone(BOGOTA).toOffsetDateTime() : null;
        jdbc.sql("""
                INSERT INTO cobros (sede_id, placa, tipo_vehiculo, dia_operativo, franja, tarifa_id,
                                    tarifa_aplicada, valor, estado, vigente_hasta)
                SELECT s.id, :plate, 'CARRO', :day, 'DIURNA', t.id, 10000, 10000, :state, CAST(:validUntil AS timestamptz)
                FROM sedes s,
                     (SELECT id FROM tarifas WHERE tipo_vehiculo = 'CARRO' AND franja = 'DIURNA' LIMIT 1) t
                WHERE s.nombre = :site
                """)
                .param("plate", plate)
                .param("day", day)
                .param("state", state)
                .param("validUntil", validUntil)
                .param("site", site)
                .update();
    }

    private void event(String site, String plate, String type, LocalDateTime at) {
        jdbc.sql("""
                INSERT INTO eventos (sede_id, placa, tipo_vehiculo, tipo_evento, origen_lectura, resultado, ocurrido_en)
                SELECT id, :plate, 'CARRO', :type, 'AUTOMATICA', 'AUTORIZADO', :at FROM sedes WHERE nombre = :site
                """)
                .param("plate", plate)
                .param("type", type)
                .param("at", at.atZone(BOGOTA).toOffsetDateTime())
                .param("site", site)
                .update();
    }
}
