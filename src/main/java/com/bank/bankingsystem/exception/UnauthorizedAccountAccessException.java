package com.bank.bankingsystem.exception;

public class UnauthorizedAccountAccessException extends RuntimeException {
    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
