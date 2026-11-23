package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.TimeSlotSchedule;
import com.parkingspot.domain.port.out.TimeSlotRepository;
import java.time.LocalTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTimeSlotRepository implements TimeSlotRepository {
    private final JdbcClient jdbc;

    public JdbcTimeSlotRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TimeSlotSchedule> findBySite(long siteId) {
        return jdbc.sql("SELECT nombre, hora_inicio, hora_fin FROM franjas WHERE sede_id = :siteId ORDER BY hora_inicio")
                .param("siteId", siteId)
                .query((row, index) -> new TimeSlotSchedule(
                        TimeSlot.valueOf(row.getString("nombre")),
                        row.getObject("hora_inicio", LocalTime.class),
                        row.getObject("hora_fin", LocalTime.class)))
                .list();
    }
}
