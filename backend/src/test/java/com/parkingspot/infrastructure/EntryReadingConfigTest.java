package com.parkingspot.infrastructure;

import com.parkingspot.domain.model.ConfidenceThreshold;
import com.parkingspot.domain.port.out.OperatorRepository;
import com.parkingspot.infrastructure.adapter.out.plate.SimulatedPlateReader;
import com.parkingspot.infrastructure.config.EntryReadingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** CA-01 de la HU-11: el umbral se configura con la variable PLATE_CONFIDENCE_THRESHOLD. */
class EntryReadingConfigTest {
    // Misma expresión que application.yml, para probar la variable de entorno sin levantar toda la aplicación.
    private static final String YAML_PROPERTY =
            "parking.entry.confidence-threshold=${PLATE_CONFIDENCE_THRESHOLD:0.80}";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EntryReadingConfig.class)
            .withBean(SimulatedPlateReader.class)
            .withBean(OperatorRepository.class, () -> username -> Optional.empty());

    @Test
    void usesDefaultThresholdWhenVariableIsAbsent() {
        runner.withPropertyValues(YAML_PROPERTY).run(context ->
                assertThat(context.getBean(ConfidenceThreshold.class).value()).isEqualTo(0.80));
    }

    @Test
    void readsThresholdFromEnvironmentVariable() {
        runner.withPropertyValues(YAML_PROPERTY, "PLATE_CONFIDENCE_THRESHOLD=0.5").run(context ->
                assertThat(context.getBean(ConfidenceThreshold.class).value()).isEqualTo(0.5));
    }

    @Test
    void failsAtStartupWithThresholdOutsideRange() {
        runner.withPropertyValues(YAML_PROPERTY, "PLATE_CONFIDENCE_THRESHOLD=1.5").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .hasMessageContaining("El umbral de confianza debe estar entre 0 y 1");
        });
    }
}
