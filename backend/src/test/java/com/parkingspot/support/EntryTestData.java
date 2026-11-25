package com.parkingspot.support;

import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore;
import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore.Role;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Deja la base de pruebas en un estado conocido: sin eventos, ocupación en cero y un operador. */
public final class EntryTestData {
    public static final String OPERATOR = "operador1";

    private EntryTestData() {
    }

    public static void reset(JdbcClient jdbc, AccountStore accounts) {
        jdbc.sql("DELETE FROM eventos").update();
        jdbc.sql("UPDATE ocupacion SET ocupados = 0").update();
        jdbc.sql("UPDATE ocupacion SET capacidad = 50 WHERE tipo_vehiculo = 'CARRO'").update();
        jdbc.sql("UPDATE ocupacion SET capacidad = 30 WHERE tipo_vehiculo = 'MOTO'").update();
        jdbc.sql("DELETE FROM usuarios").update();
        accounts.create(OPERATOR, "Operador de prueba", Role.OPERADOR, "clave-de-prueba");
    }

    public static long operatorId(JdbcClient jdbc) {
        return jdbc.sql("SELECT id FROM usuarios WHERE username = :u").param("u", OPERATOR).query(Long.class).single();
    }

    public static long siteId(JdbcClient jdbc) {
        return jdbc.sql("SELECT id FROM sedes WHERE nombre = 'Sede Principal'").query(Long.class).single();
    }

    public static int occupied(JdbcClient jdbc, String vehicleType) {
        return jdbc.sql("SELECT ocupados FROM ocupacion WHERE tipo_vehiculo = :t")
                .param("t", vehicleType).query(Integer.class).single();
    }

    public static int events(JdbcClient jdbc) {
        return jdbc.sql("SELECT COUNT(*) FROM eventos").query(Integer.class).single();
    }
}
