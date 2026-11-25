package com.parkingspot.domain.port.out;

import com.parkingspot.domain.model.EntryEvent;
import java.time.LocalDate;

/** Guarda los eventos de ingreso (tabla eventos de la HU-05). */
public interface EntryEventRepository {
    /** Guarda el evento y devuelve su identificador. */
    long save(EntryEvent event, LocalDate exemptOperatingDay);
}
