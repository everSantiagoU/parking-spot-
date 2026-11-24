package com.parkingspot.infrastructure;

import com.parkingspot.application.usecase.RegisterEntryUseCase;
import com.parkingspot.domain.model.EntryAttempt;
import com.parkingspot.domain.model.EntryOutcome;
import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore;
import com.parkingspot.support.EntryIntegrationTest;
import com.parkingspot.support.EntryTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-13 CA-04: ingresos simultáneos no superan la capacidad.
 * 20 hilos intentan entrar al mismo tiempo a una sede con 5 cupos para carro. Todos esperan en una
 * "línea de salida" (CountDownLatch) y arrancan a la vez, usando transacciones y conexiones reales.
 */
@EntryIntegrationTest
class ConcurrentEntryTest {
    private static final int CAPACITY = 5;
    private static final int VEHICLES = 20;

    @Autowired RegisterEntryUseCase registerEntry;
    @Autowired AccountStore accounts;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        EntryTestData.reset(jdbc, accounts);
        jdbc.sql("UPDATE ocupacion SET capacidad = :c WHERE tipo_vehiculo = 'CARRO'").param("c", CAPACITY).update();
    }

    @RepeatedTest(5)
    void simultaneousEntriesNeverExceedCapacity() throws Exception {
        var startLine = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(VEHICLES);
        try {
            List<Future<EntryAttempt>> attempts = new ArrayList<>();
            for (int i = 0; i < VEHICLES; i++) {
                String plate = "CAR%03d".formatted(i);
                Callable<EntryAttempt> entry = () -> {
                    startLine.await();
                    return registerEntry.enterManual(plate, EntryTestData.OPERATOR);
                };
                attempts.add(executor.submit(entry));
            }
            startLine.countDown();

            int authorized = 0;
            int full = 0;
            for (var attempt : attempts) {
                var outcome = attempt.get(30, TimeUnit.SECONDS).outcome();
                if (outcome == EntryOutcome.AUTORIZADO) {
                    authorized++;
                } else if (outcome == EntryOutcome.LLENO) {
                    full++;
                }
            }

            assertThat(authorized).isEqualTo(CAPACITY);
            assertThat(full).isEqualTo(VEHICLES - CAPACITY);
            assertThat(EntryTestData.occupied(jdbc, "CARRO")).isEqualTo(CAPACITY);
            assertThat(countEvents("AUTORIZADO")).isEqualTo(CAPACITY);
            assertThat(countEvents("RECHAZADO")).isEqualTo(VEHICLES - CAPACITY);
        } finally {
            executor.shutdownNow();
        }
    }

    private int countEvents(String result) {
        return jdbc.sql("SELECT COUNT(*) FROM eventos WHERE resultado = :r").param("r", result)
                .query(Integer.class).single();
    }
}
