package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.domain.model.Confidence;
import com.parkingspot.domain.model.ReadingSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapeo del dominio al esquema de eventos, sin cambiar el contrato de la HU-08. */
class EntryEventStoreMappingTest {

    @Test
    void mapsReadingSourceToSchemaOrigin() {
        assertThat(EntryEventStore.readingOrigin(ReadingSource.SIMULADOR)).isEqualTo("AUTOMATICA");
        assertThat(EntryEventStore.readingOrigin(ReadingSource.MANUAL)).isEqualTo("MANUAL");
    }

    @Test
    void storesConfidenceAsPercentageWithTwoDecimals() {
        assertThat(EntryEventStore.percentage(new Confidence(0.93))).isEqualByComparingTo("93.00");
        assertThat(EntryEventStore.percentage(new Confidence(0.33333))).isEqualByComparingTo("33.33");
        assertThat(EntryEventStore.percentage(new Confidence(0.0))).isEqualByComparingTo("0.00");
        assertThat(EntryEventStore.percentage(new Confidence(1.0))).isEqualByComparingTo("100.00");
        assertThat(EntryEventStore.percentage(null)).isNull();
    }
}
