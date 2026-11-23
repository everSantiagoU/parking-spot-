package com.parkingspot.infrastructure.adapter.out.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

@Repository
public class AccountStore implements UserDetailsService {
    private final JdbcClient jdbc;
    private final PasswordEncoder passwords;

    public AccountStore(JdbcClient jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return jdbc.sql("""
                SELECT u.username, u.password_hash, u.activo, r.nombre AS rol
                FROM usuarios u JOIN roles r ON r.id = u.rol_id
                WHERE u.username = :username
                """)
                .param("username", username)
                .query((row, index) -> User.withUsername(row.getString("username"))
                        .password(row.getString("password_hash"))
                        .roles(row.getString("rol"))
                        .disabled(!row.getBoolean("activo"))
                        .build())
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Usuario desconocido"));
    }

    public void create(String username, String fullName, Role role, CharSequence password) {
        if (password.isEmpty() || StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).remaining() > 72) {
            throw new IllegalArgumentException("La contraseña debe contener entre 1 y 72 bytes UTF-8.");
        }
        int created = jdbc.sql("""
                INSERT INTO usuarios (username, nombre_completo, rol_id, sede_id, password_hash)
                SELECT :username, :name, r.id, s.id, :hash
                FROM roles r
                LEFT JOIN sedes s ON r.nombre = 'OPERADOR' AND s.nombre = 'Sede Principal' AND s.activa
                WHERE r.nombre = :role AND (r.nombre = 'ADMIN' OR s.id IS NOT NULL)
                """)
                .param("username", username)
                .param("name", fullName)
                .param("role", role.name())
                .param("hash", passwords.encode(password))
                .update();
        if (created != 1) {
            throw new IllegalStateException("Aplica el esquema y los datos iniciales de HU-05.");
        }
    }

    public enum Role { OPERADOR, ADMIN }
}
