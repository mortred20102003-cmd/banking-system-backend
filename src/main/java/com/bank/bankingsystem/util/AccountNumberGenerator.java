package com.bank.bankingsystem.util;

import com.bank.bankingsystem.exception.BusinessRuleException;
import com.bank.bankingsystem.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique 10-digit account numbers.
 * Format: <prefix><random digits> — prefix defaults to "10".
 * Uniqueness is verified against the DB; collisions retry up to 10 times.
 */
@Component
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 10;

    private final AccountRepository accountRepository;
    private final String prefix;
    private final int length;

    public AccountNumberGenerator(
            AccountRepository accountRepository,
            @Value("${app.account.number-prefix:10}") String prefix,
            @Value("${app.account.number-length:10}") int length) {
        this.accountRepository = accountRepository;
        this.prefix = prefix;
        this.length = length;
    }

    public String generate() {
        int randomDigits = length - prefix.length();
        if (randomDigits <= 0) {
            throw new BusinessRuleException(
                    "ACCOUNT_NUMBER_LENGTH must exceed ACCOUNT_NUMBER_PREFIX length");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            sb.setLength(0);
            sb.append(prefix);
            for (int i = 0; i < randomDigits; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new BusinessRuleException(
                "Could not generate a unique account number after " + MAX_ATTEMPTS + " attempts");
    }
}