package com.parkingspot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * HU-21 · CA-04 y CP-HU-21-X01: revisa los imports del código para detectar dependencias
 * que rompen la arquitectura hexagonal. Si alguien importa Spring, JPA o un adaptador
 * desde el dominio o la aplicación, esta prueba falla e indica el archivo y la línea.
 */
class ArchitectureTest {
    private static final Path SOURCES = Path.of("src/main/java/com/parkingspot");

    @Test
    void elDominioNoDependeDeFrameworksNiDeOtrasCapas() throws IOException {
        var violations = importsIn("domain", List.of(
                "org.springframework", "jakarta.persistence", "org.slf4j",
                "com.parkingspot.application", "com.parkingspot.infrastructure"));

        assertThat(violations).as("imports prohibidos en domain").isEmpty();
    }

    @Test
    void laAplicacionNoDependeDeSpringNiDeLosAdaptadores() throws IOException {
        var violations = importsIn("application", List.of(
                "org.springframework", "jakarta.persistence", "com.parkingspot.infrastructure"));

        assertThat(violations).as("imports prohibidos en application").isEmpty();
    }

    /** Devuelve "archivo: import" por cada import que empiece por un paquete prohibido. */
    private static List<String> importsIn(String layer, List<String> forbidden) throws IOException {
        try (Stream<Path> files = Files.walk(SOURCES.resolve(layer))) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .flatMap(file -> forbiddenImports(file, forbidden))
                    .toList();
        }
    }

    private static Stream<String> forbiddenImports(Path file, List<String> forbidden) {
        try {
            return Files.readAllLines(file).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .filter(line -> forbidden.stream().anyMatch(prefix -> line.contains(" " + prefix)
                            || line.contains("static " + prefix)))
                    .map(line -> SOURCES.relativize(file) + ": " + line);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer " + file, exception);
        }
    }
}
