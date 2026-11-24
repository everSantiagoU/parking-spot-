package com.parkingspot.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Prueba de integración del flujo de ingreso con la aplicación completa, MockMvc, la seguridad real
 * y H2 en modo PostgreSQL (igual que AuthenticationTest), con las tablas de entry-schema.sql y un
 * reloj fijo ({@link FixedClockConfig#NOW}). Todas las clases que la usan comparten un solo contexto.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:entries;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:auth-schema.sql,classpath:entry-schema.sql",
        "spring.flyway.enabled=false",
        "parking.entry.confidence-threshold=0.80"
})
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
public @interface EntryIntegrationTest {
}
