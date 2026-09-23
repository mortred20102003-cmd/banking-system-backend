package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.CreateAccountRequest;
import com.bank.bankingsystem.dto.response.AccountResponse;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.enums.AccountStatus;
import com.bank.bankingsystem.model.enums.AccountType;
import com.bank.bankingsystem.model.enums.Role;
import com.bank.bankingsystem.model.enums.UserStatus;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.security.AuthenticatedUser;
import com.bank.bankingsystem.service.impl.AccountServiceImpl;
import com.bank.bankingsystem.util.AccountNumberGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private AccountNumberGenerator generator;
    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        customerRepository = mock(CustomerRepository.class);
        generator = mock(AccountNumberGenerator.class);
        service = new AccountServiceImpl(accountRepository, customerRepository, generator);
        setCurrentUser(1L, "alice", Role.CUSTOMER, UserStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAccount_createsActiveSavingsAccountWithZeroBalance() {
        Customer owner = customer(10L, 1L);
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(owner));
        when(generator.generate()).thenReturn("1000000042");

        Account saved = new Account();
        saved.setId(100L);
        saved.setCustomerId(10L);
        saved.setAccountNumber("1000000042");
        saved.setAccountType(AccountType.SAVINGS);
        saved.setBalance(BigDecimal.ZERO.setScale(4));
        saved.setStatus(AccountStatus.ACTIVE);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        when(accountRepository.insert(any(Account.class))).thenReturn(100L);
        when(accountRepository.findById(100L)).thenReturn(Optional.of(saved));

        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);

        AccountResponse resp = service.createAccount(req);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getAccountNumber()).isEqualTo("1000000042");
        assertThat(resp.getAccountType()).isEqualTo("SAVINGS");
        assertThat(resp.getStatus()).isEqualTo("ACTIVE");
        assertThat(resp.getBalance()).isEqualByComparingTo("0.0000");

        verify(accountRepository).insert(any(Account.class));
    }

    @Test
    void createAccount_failsWhenNoCustomerProfile() {
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.CHECKING);

        assertThatThrownBy(() -> service.createAccount(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer profile");
    }

    // ----- helpers -----

    private static Customer customer(Long id, Long userId) {
        Customer c = new Customer();
        c.setId(id);
        c.setUserId(userId);
        c.setFirstName("Alice");
        c.setLastName("Smith");
        return c;
    }

    private static void setCurrentUser(Long id, String username, Role role, UserStatus status) {
        com.bank.bankingsystem.model.User u = new com.bank.bankingsystem.model.User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPasswordHash("x");
        u.setRole(role);
        u.setStatus(status);
        AuthenticatedUser principal = new AuthenticatedUser(u);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}