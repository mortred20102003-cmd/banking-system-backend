package com.bank.bankingsystem.integration;

import com.bank.bankingsystem.dto.request.LoginRequest;
import com.bank.bankingsystem.dto.request.RegisterRequest;
import com.bank.bankingsystem.dto.response.AuthResponse;
import com.bank.bankingsystem.exception.AuthenticationException;
import com.bank.bankingsystem.exception.DuplicateResourceException;
import com.bank.bankingsystem.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired AuthService authService;

    @Test
    void register_then_login_success() {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("charlie_it");
        reg.setEmail("charlie_it@test.com");
        reg.setPassword("Password123!");
        reg.setFirstName("Charlie");
        reg.setLastName("Tester");

        AuthResponse registered = authService.register(reg);
        assertThat(registered.getToken()).isNotBlank();
        assertThat(registered.getRole()).isEqualTo("CUSTOMER");

        LoginRequest login = new LoginRequest();
        login.setUsernameOrEmail("charlie_it");
        login.setPassword("Password123!");

        AuthResponse loggedIn = authService.login(login);
        assertThat(loggedIn.getToken()).isNotBlank();
        assertThat(loggedIn.getUserId()).isEqualTo(registered.getUserId());
    }

    @Test
    void register_rejectsDuplicateUsername() {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("dupuser");
        reg.setEmail("dup1@test.com");
        reg.setPassword("Password123!");
        reg.setFirstName("Dup");
        reg.setLastName("One");
        authService.register(reg);

        RegisterRequest dup = new RegisterRequest();
        dup.setUsername("dupuser");
        dup.setEmail("dup2@test.com"); // different email
        dup.setPassword("Password123!");
        dup.setFirstName("Dup");
        dup.setLastName("Two");

        assertThatThrownBy(() -> authService.register(dup))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void login_wrongPassword_fails() {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("wrongpass");
        reg.setEmail("wrongpass@test.com");
        reg.setPassword("Password123!");
        reg.setFirstName("Wrong");
        reg.setLastName("Pass");
        authService.register(reg);

        LoginRequest bad = new LoginRequest();
        bad.setUsernameOrEmail("wrongpass");
        bad.setPassword("not-the-password");

        assertThatThrownBy(() -> authService.login(bad))
                .isInstanceOf(AuthenticationException.class);
    }
}