package com.parkingspot.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingspot.infrastructure.adapter.in.rest.AuthenticationController.SessionUser;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String LOGOUT = "/api/v1/auth/logout";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, CorsConfigurationSource cors, ObjectMapper json)
            throws Exception {
        return http
                .cors(config -> config.configurationSource(cors))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf", "/actuator", "/actuator/health",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, LOGIN).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/vehicles/classify").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/plate-readings/simulate").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/entries/automatic").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/entries/manual").hasRole("OPERADOR")
