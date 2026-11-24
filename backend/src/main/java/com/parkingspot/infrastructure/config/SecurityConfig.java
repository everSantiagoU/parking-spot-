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
                        .requestMatchers(HttpMethod.POST, "/api/v1/charges").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/charges/today").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/charge/*").hasRole("OPERADOR")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> {
                            // CSRF se comprueba antes de que Spring instale la autenticación anónima.
                            var current = SecurityContextHolder.getContext().getAuthentication();
                            if ((current == null || current instanceof AnonymousAuthenticationToken)
                                    && !(request.getContextPath() + LOGIN).equals(request.getRequestURI())) {
                                response.setStatus(401);
                            } else {
                                writeError(response, json, 403, "ACCESS_DENIED", "Acceso denegado.");
                            }
                        }))
                .formLogin(form -> form.loginPage(LOGIN).loginProcessingUrl(LOGIN)
                        .successHandler((request, response, authentication) ->
                                SessionUser.from(authentication).writeTo(response, json))
                        .failureHandler((request, response, exception) ->
                                writeError(response, json, 401, "INVALID_CREDENTIALS", "Credenciales inválidas.")))
                .logout(logout -> logout.logoutUrl(LOGOUT).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(authentication == null ? 401 : 204)))
                .build();
    }

    @Bean
    CorsConfigurationSource cors(@Value("${FRONTEND_ORIGIN:http://localhost:5173}") String origin) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private static void writeError(HttpServletResponse response, ObjectMapper json, int status, String code,
                                   String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), Map.of("code", code, "message", message));
    }
}
