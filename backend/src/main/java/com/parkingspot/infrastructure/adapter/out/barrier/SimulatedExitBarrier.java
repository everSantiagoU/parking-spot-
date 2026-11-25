package com.parkingspot.infrastructure.adapter.out.barrier;

import com.parkingspot.domain.port.out.ExitBarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Patrón Adapter: adapta el puerto {@link ExitBarrier} a una barrera simulada que solo deja
 * constancia en el log. Una barrera física tendría su propio adaptador y bastaría con cambiar
 * el bean en {@code ExitConfig}; el dominio y la aplicación no cambian.
 */
public class SimulatedExitBarrier implements ExitBarrier {
    private static final Logger log = LoggerFactory.getLogger(SimulatedExitBarrier.class);

    @Override
    public void open(long siteId, String plate) {
        log.info("Apertura simulada de barrera de salida en sede {} para {}", siteId, plate);
    }
}
