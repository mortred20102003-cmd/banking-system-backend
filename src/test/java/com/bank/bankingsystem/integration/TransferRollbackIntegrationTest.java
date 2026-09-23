package com.bank.bankingsystem.integration;

import com.bank.bankingsystem.dto.request.RegisterRequest;
import com.bank.bankingsystem.dto.request.TransferRequest;
import com.bank.bankingsystem.dto.response.AccountResponse;
import com.bank.bankingsystem.dto.response.AuthResponse;
import com.bank.bankingsystem.dto.request.CreateAccountRequest;
import com.bank.bankingsystem.model.enums.AccountType;
import com.bank.bankingsystem.model.enums.Role;
import com.bank.bankingsystem.model.enums.UserStatus;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.UserRepository;
import com.bank.bankingsystem.service.AccountService;
import com.bank.bankingsystem.service.AuthService;
import com.bank.bankingsystem.service.TransferService;
import com.bank.bankingsystem.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TransferRollbackIntegrationTest {

    @Autowired AuthService authService;
    @Autowired AccountService accountService;
    @Autowired TransferService transferService;
    @Autowired AccountRepository accountRepository;
    @Autowired UserRepository userRepository;

    // Spy on the real bean so we can override just the credit call
    @SpyBean AccountRepository spyAccountRepo;

    @BeforeEach
    void resetMocks() {
        reset(spyAccountRepo);
    }

    @Test
    void transfer_rollsBackDebitWhenCreditFails() {
        // --- setup two customers with one account each ---
        AuthResponse alice = authService.register(register("alice_rollback", "alice_rb@test.com", "Alice"));
        AuthResponse bob   = authService.register(register("bob_rollback",   "bob_rb@test.com",   "Bob"));

        // set security context to alice to create her account
        authenticateAs(alice.getUserId());
        AccountResponse aliceAcct = accountService.createAccount(createAccount(AccountType.SAVINGS));

        // deposit 500 into alice (as alice)
        accountRepository.credit(aliceAcct.getId(), new BigDecimal("500.00"));
        BigDecimal aliceBalanceBefore = accountRepository.findById(aliceAcct.getId()).orElseThrow().getBalance();
        assertThat(aliceBalanceBefore).isEqualByComparingTo("500.0000");

        // create bob's account — authenticate as bob
        authenticateAs(bob.getUserId());
        AccountResponse bobAcct = accountService.createAccount(createAccount(AccountType.SAVINGS));

        // back to alice for the transfer
        authenticateAs(alice.getUserId());

        // --- make the destination credit fail ---
        // The first credit call in this test would be on the destination account.
        // Force the repository to return 0 rows affected for the credit call.
        doReturn(0).when(spyAccountRepo).credit(eq(bobAcct.getId()), any(BigDecimal.class));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber(aliceAcct.getAccountNumber());
        req.setDestinationAccountNumber(bobAcct.getAccountNumber());
        req.setAmount(new BigDecimal("100.00"));
        req.setDescription("rollback test");

        assertThatThrownBy(() -> transferService.transfer(req))
                .hasMessageContaining("destination");

        // --- CRITICAL ASSERTION ---
        // Alice's balance must be UNCHANGED after the rollback.
        BigDecimal aliceBalanceAfter = accountRepository.findById(aliceAcct.getId()).orElseThrow().getBalance();
        assertThat(aliceBalanceAfter)
                .as("Source balance must be rolled back when destination credit fails")
                .isEqualByComparingTo(aliceBalanceBefore);
    }

    // ---- helpers ----

    private static RegisterRequest register(String username, String email, String firstName) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword("Password123!");
        r.setFirstName(firstName);
        r.setLastName("Test");
        return r;
    }

    private static CreateAccountRequest createAccount(AccountType type) {
        CreateAccountRequest r = new CreateAccountRequest();
        r.setAccountType(type);
        return r;
    }

    private void authenticateAs(Long userId) {
        User u = userRepository.findById(userId).orElseThrow();
        var principal = new com.bank.bankingsystem.security.AuthenticatedUser(u);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}