package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.VehicleType;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Adaptador de persistencia de la configuración del parqueadero (HU-12): lee y guarda tarifas,
 * franjas y cupos con JdbcClient. Está en infraestructura porque usa Spring y SQL directamente;
 * antes estaba en application y rompía la regla de la arquitectura hexagonal (HU-21, CP-HU-21-X01).
 */
@Service
public class ParkingConfigurationStore {
    public record Site(long id, String name) {}
    public record Rates(BigDecimal day, BigDecimal night, Integer capacity) {}
    public record Configuration(LocalTime dayStart, LocalTime nightStart, Rates car, Rates motorcycle) {}

    private final JdbcClient jdbc;

    public ParkingConfigurationStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Site> sites() {
        return jdbc.sql("SELECT id, nombre FROM sedes WHERE activa ORDER BY nombre, id")
                .query((row, i) -> new Site(row.getLong("id"), row.getString("nombre"))).list();
    }

    private void requireSite(long site) {
        if (!jdbc.sql("SELECT EXISTS(SELECT 1 FROM sedes WHERE id = :site AND activa)")
                .param("site", site).query(Boolean.class).single()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no disponible.");
        }
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Configuration read(long site) {
        requireSite(site);
        return new Configuration(start(site, "DIURNA"), start(site, "NOCTURNA"),
                rates(site, VehicleType.CARRO), rates(site, VehicleType.MOTO));
    }

    private LocalTime start(long site, String slot) {
        return jdbc.sql("SELECT hora_inicio FROM franjas WHERE sede_id = :site AND nombre = :slot")
                .param("site", site).param("slot", slot).query(LocalTime.class).optional().orElse(null);
    }

    private Rates rates(long site, VehicleType type) {
        var capacity = jdbc.sql("SELECT capacidad FROM ocupacion WHERE sede_id = :site AND tipo_vehiculo = :type")
                .param("site", site).param("type", type.name()).query(Integer.class).optional().orElse(null);
        return new Rates(value(site, type, "DIURNA"), value(site, type, "NOCTURNA"), capacity);
    }

    private BigDecimal value(long site, VehicleType type, String slot) {
        return jdbc.sql("SELECT valor FROM tarifas WHERE sede_id = :site AND tipo_vehiculo = :type AND franja = :slot")
                .param("site", site).param("type", type.name()).param("slot", slot)
                .query(BigDecimal.class).optional().orElse(null);
    }

    @Transactional
    public void save(long site, Configuration data) {
        requireSite(site);
        if (data.dayStart() == null || data.nightStart() == null || data.dayStart().equals(data.nightStart())) {
            throw invalid("Las franjas deben empezar a horas distintas.");
        }
        saveRates(site, VehicleType.CARRO, data.car());
        saveRates(site, VehicleType.MOTO, data.motorcycle());
        saveSlot(site, "DIURNA", data.dayStart(), data.nightStart());
        saveSlot(site, "NOCTURNA", data.nightStart(), data.dayStart());
    }

    private void saveRates(long site, VehicleType type, Rates rates) {
        if (rates == null || rates.capacity() == null || rates.capacity() < 0
                || !validAmount(rates.day()) || !validAmount(rates.night())) {
            throw invalid("Revisa los importes y la capacidad de " + type + ".");
        }
        boolean exempt = type == VehicleType.MOTO && rates.day().signum() == 0 && rates.night().signum() == 0;
        if (!exempt && (rates.night().compareTo(rates.day()) >= 0
                || (type == VehicleType.CARRO && rates.night().signum() == 0))) {
            throw invalid("La tarifa nocturna debe ser menor que la diurna. El carro requiere tarifas positivas.");
        }
        int changed = jdbc.sql("""
                INSERT INTO ocupacion (sede_id, tipo_vehiculo, capacidad) VALUES (:site, :type, :capacity)
                ON CONFLICT (sede_id, tipo_vehiculo) DO UPDATE
                SET capacidad = EXCLUDED.capacidad, actualizada_en = CURRENT_TIMESTAMP
                WHERE ocupacion.ocupados <= EXCLUDED.capacidad
                """).param("site", site).param("type", type.name()).param("capacity", rates.capacity()).update();
        if (changed != 1) throw invalid("La capacidad de " + type + " es inferior a la ocupación actual.");
        saveTariff(site, type, "DIURNA", rates.day());
        saveTariff(site, type, "NOCTURNA", rates.night());
    }

    private boolean validAmount(BigDecimal value) {
        return value != null && value.signum() >= 0 && value.stripTrailingZeros().scale() <= 2
                && value.compareTo(new BigDecimal("10000000000")) < 0;
    }

    private void saveTariff(long site, VehicleType type, String slot, BigDecimal value) {
        jdbc.sql("""
                INSERT INTO tarifas (sede_id, tipo_vehiculo, franja, valor) VALUES (:site, :type, :slot, :value)
                ON CONFLICT (sede_id, tipo_vehiculo, franja) DO UPDATE
                SET valor = EXCLUDED.valor, actualizada_en = CURRENT_TIMESTAMP
                """).param("site", site).param("type", type.name()).param("slot", slot).param("value", value).update();
    }

    private void saveSlot(long site, String slot, LocalTime start, LocalTime end) {
        jdbc.sql("""
                INSERT INTO franjas (sede_id, nombre, hora_inicio, hora_fin) VALUES (:site, :slot, :start, :end)
                ON CONFLICT (sede_id, nombre) DO UPDATE SET hora_inicio = EXCLUDED.hora_inicio, hora_fin = EXCLUDED.hora_fin
                """).param("site", site).param("slot", slot).param("start", start).param("end", end).update();
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
