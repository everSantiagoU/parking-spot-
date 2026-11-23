package com.parkingspot.infrastructure.adapter.in.rest;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    @GetMapping("/csrf")
    CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/me")
    SessionUser me(Authentication authentication) {
        return SessionUser.from(authentication);
    }

    public record SessionUser(String username, String role) {
        public static SessionUser from(Authentication authentication) {
            var authority = authentication.getAuthorities().iterator().next().getAuthority();
            return new SessionUser(authentication.getName(), authority.substring("ROLE_".length()));
        }

        public void writeTo(HttpServletResponse response, ObjectMapper json) throws IOException {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(), this);
        }
    }
}
