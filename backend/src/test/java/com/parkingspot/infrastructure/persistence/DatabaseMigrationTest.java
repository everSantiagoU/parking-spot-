package com.parkingspot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HU-05: verifica que las migraciones de Flyway crean el esquema desde una base vacía
 * y que las restricciones de la base de datos protegen la integridad de la operación.
 * Usa un PostgreSQL real en Docker (Testcontainers). Si Docker no está disponible,
 * las pruebas se omiten en lugar de fallar.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String INSERT_COBRO = """
            INSERT INTO cobros (sede_id, placa, tipo_vehiculo, dia_operativo, franja, tarifa_id, tarifa_aplicada, valor)
            VALUES (?, ?, 'CARRO', ?::date, 'DIURNA', ?, 10000, 10000)
            """;

    @Autowired
    private JdbcTemplate jdbc;

    private long sedeId;
    private long tarifaCarroDiurnaId;

    @BeforeEach
    void limpiarDatosDePrueba() {
        jdbc.update("DELETE FROM eventos");
        jdbc.update("DELETE FROM pagos");
        jdbc.update("DELETE FROM cobros");
        jdbc.update("DELETE FROM usuarios");
        sedeId = jdbc.queryForObject("SELECT id FROM sedes WHERE nombre = 'Sede Principal'", Long.class);
        tarifaCarroDiurnaId = jdbc.queryForObject(
                "SELECT id FROM tarifas WHERE tipo_vehiculo = 'CARRO' AND franja = 'DIURNA'", Long.class);
    }

    @Test
    @DisplayName("CA-01 y CA-04: las migraciones crean todas las tablas desde una base vacía")
    void migracionesCreanTodasLasTablas() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tablas).contains(
                "usuarios", "roles", "sedes", "eventos", "tarifas", "cobros", "pagos", "ocupacion", "franjas");
    }

    @Test
    @DisplayName("CA-04: Flyway registra las migraciones V1 a V4 como exitosas")
    void flywayRegistraLasMigraciones() {
        List<String> versiones = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versiones).containsExactly("1", "2", "3", "4");
    }

    @Test
    @DisplayName("Datos iniciales: roles, tarifa de moto en 0 (RN-01) y nocturna menor que diurna (RN-02)")
    void datosInicialesCumplenLasReglas() {
        assertThat(jdbc.queryForList("SELECT nombre FROM roles", String.class))
                .containsExactlyInAnyOrder("ADMIN", "OPERADOR");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM tarifas WHERE tipo_vehiculo = 'MOTO' AND valor <> 0", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("""
                SELECT n.valor < d.valor FROM tarifas n JOIN tarifas d ON d.sede_id = n.sede_id
                WHERE n.tipo_vehiculo = 'CARRO' AND n.franja = 'NOCTURNA'
                  AND d.tipo_vehiculo = 'CARRO' AND d.franja = 'DIURNA'
                """, Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("CA-03: no se permiten dos cobros para la misma placa, sede y día operativo")
    void cobroDuplicadoEsRechazado() {
        jdbc.update(INSERT_COBRO, sedeId, "ABC123", "2026-11-21", tarifaCarroDiurnaId);

        assertThatThrownBy(() -> jdbc.update(INSERT_COBRO, sedeId, "ABC123", "2026-11-21", tarifaCarroDiurnaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_cobros_placa_sede_dia");
    }

    @Test
    @DisplayName("CA-03: la misma placa sí puede tener cobro en otro día operativo")
    void mismaPlacaOtroDiaEsPermitida() {
        jdbc.update(INSERT_COBRO, sedeId, "ABC123", "2026-11-21", tarifaCarroDiurnaId);
        jdbc.update(INSERT_COBRO, sedeId, "ABC123", "2026-11-22", tarifaCarroDiurnaId);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cobros WHERE placa = 'ABC123'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("CA-02: un cobro no puede apuntar a una sede inexistente")
    void cobroConSedeInexistenteEsRechazado() {
        assertThatThrownBy(() -> jdbc.update(INSERT_COBRO, 999_999L, "ABC123", "2026-11-21", tarifaCarroDiurnaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_cobros_sede");
    }

    @Test
    @DisplayName("CA-02: un usuario no puede tener un rol inexistente")
    void usuarioConRolInexistenteEsRechazado() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id)
                VALUES ('fantasma', 'hash', 'Usuario', 999999)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_usuarios_rol");
    }

    @Test
    @DisplayName("CA-02: un pago no puede referenciar un cobro inexistente")
    void pagoConCobroInexistenteEsRechazado() {
        long usuarioId = crearOperador();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO pagos (cobro_id, valor, usuario_id, clave_idempotencia)
                VALUES (999999, 10000, ?, 'clave-1')
                """, usuarioId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_pagos_cobro");
    }

    @Test
    @DisplayName("Un cobro admite un solo pago y la clave de idempotencia no se repite")
    void pagoDuplicadoEsRechazado() {
        long usuarioId = crearOperador();
        jdbc.update(INSERT_COBRO, sedeId, "ABC123", "2026-11-21", tarifaCarroDiurnaId);
        long cobroId = jdbc.queryForObject("SELECT id FROM cobros WHERE placa = 'ABC123'", Long.class);
        String insertPago = "INSERT INTO pagos (cobro_id, valor, usuario_id, clave_idempotencia) VALUES (?, 10000, ?, ?)";
        jdbc.update(insertPago, cobroId, usuarioId, "clave-1");

        assertThatThrownBy(() -> jdbc.update(insertPago, cobroId, usuarioId, "clave-2"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_pagos_cobro");
    }

    @Test
    @DisplayName("Un cobro PAGADO debe tener la fecha de vencimiento del pase (RN-03)")
    void cobroPagadoSinVigenciaEsRechazado() {
        jdbc.update(INSERT_COBRO, sedeId, "ABC123", "2026-11-21", tarifaCarroDiurnaId);

        assertThatThrownBy(() -> jdbc.update("UPDATE cobros SET estado = 'PAGADO' WHERE placa = 'ABC123'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cobros_pase_pagado");
    }

    @Test
    @DisplayName("Placas con formato inválido son rechazadas")
    void placaInvalidaEsRechazada() {
        assertThatThrownBy(() -> jdbc.update(INSERT_COBRO, sedeId, "abc12", "2026-11-21", tarifaCarroDiurnaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cobros_placa");
    }

    @Test
    @DisplayName("La ocupación nunca supera la capacidad")
    void ocupacionMayorQueCapacidadEsRechazada() {
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE ocupacion SET ocupados = capacidad + 1 WHERE tipo_vehiculo = 'CARRO'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_ocupacion_ocupados");
    }

    private long crearOperador() {
        jdbc.update("""
                INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, sede_id)
                VALUES ('operador.prueba', 'hash', 'Operador de prueba',
                        (SELECT id FROM roles WHERE nombre = 'OPERADOR'), ?)
                """, sedeId);
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE username = 'operador.prueba'", Long.class);
    }
}
