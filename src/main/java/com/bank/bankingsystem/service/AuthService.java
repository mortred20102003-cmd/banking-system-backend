package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.LoginRequest;
import com.bank.bankingsystem.dto.request.RegisterRequest;
import com.bank.bankingsystem.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Registers a new CUSTOMER user and their customer profile atomically.
     * Returns the same shape as login so the client is immediately usable.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Verifies credentials and issues a JWT.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Generates a one-time reset token for the given email.
     * The token is logged to the server console (no email infra in this project).
     * Always returns normally, even for unknown emails, to avoid account enumeration.
     */
    void requestPasswordReset(String email);

    /**
     * Consumes a reset token and updates the user's password.
     * Token must exist, be unused, and not be expired.
     */
    void resetPassword(String token, String newPassword);
}
