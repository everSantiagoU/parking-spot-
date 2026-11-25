package com.parkingspot.infrastructure.adapter.out.barrier;

import com.parkingspot.domain.port.out.ExitBarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedExitBarrier implements ExitBarrier {
    private static final Logger log = LoggerFactory.getLogger(SimulatedExitBarrier.class);

    @Override
    public void open(long siteId, String plate) {
        log.info("Apertura simulada de barrera de salida en sede {} para {}", siteId, plate);
    }
}
