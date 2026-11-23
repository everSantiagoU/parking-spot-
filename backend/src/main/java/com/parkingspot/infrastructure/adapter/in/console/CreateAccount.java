package com.parkingspot.infrastructure.adapter.in.console;

import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore;
import com.parkingspot.infrastructure.adapter.out.persistence.AccountStore.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.CharBuffer;
import java.util.Arrays;

@Component
@Profile("create-account")
@ConditionalOnNotWebApplication
public class CreateAccount implements CommandLineRunner {
    private final AccountStore accounts;

    public CreateAccount(AccountStore accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run(String... args) {
        var console = System.console();
        if (console == null) {
            throw new IllegalStateException("Ejecuta el JAR desde una terminal interactiva.");
        }
        Role role = Role.valueOf(console.readLine("Rol (OPERADOR o ADMIN): ").strip());
        String username = console.readLine("Usuario: ").strip();
        String fullName = console.readLine("Nombre completo: ").strip();
        if (username.isEmpty() || username.length() > 50 || fullName.isEmpty() || fullName.length() > 120) {
            throw new IllegalArgumentException("Nombre de usuario o nombre completo inválido.");
        }
        char[] password = console.readPassword("Contraseña: ");
        if (password == null) {
            throw new IllegalArgumentException("Falta la contraseña.");
        }
        try {
            accounts.create(username, fullName, role, CharBuffer.wrap(password));
        } finally {
            Arrays.fill(password, '\0');
        }
        console.printf("Usuario creado.%n");
    }
}
