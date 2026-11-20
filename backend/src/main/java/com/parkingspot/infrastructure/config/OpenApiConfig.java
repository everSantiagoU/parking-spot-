package com.parkingspot.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI parkingSpotOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Parking Spot API")
                .description("API inicial del proyecto Parking Spot.")
                .version("0.0.1"));
    }

    @Bean
    GlobalOpenApiCustomizer actuatorHealthDocumentation() {
        return openApi -> {
            if (openApi.getPaths() == null || openApi.getPaths().get("/actuator/health") == null) {
                return;
            }
            var health = openApi.getPaths().get("/actuator/health").getGet();
            if (health == null) {
                return;
            }
            health.setSummary("Estado de salud de la aplicación");
            health.setDescription("Consulta el estado agregado que publica Spring Boot Actuator.");
            health.getResponses().get("200").setDescription("Estado UP u otro estado sin asignación HTTP de error.");
            health.getResponses().addApiResponse("503", new ApiResponse()
                    .description("Estado DOWN u OUT_OF_SERVICE con la configuración predeterminada de Actuator."));
        };
    }
}
