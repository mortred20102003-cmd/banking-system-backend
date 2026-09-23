package com.bank.bankingsystem.exception;

/**
 * Thrown by our auth service for bad credentials.
 * Named "AuthenticationException" — intentional, distinct from Spring's
 * org.springframework.security.core.AuthenticationException.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}