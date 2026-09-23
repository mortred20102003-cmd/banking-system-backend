package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.DepositRequest;
import com.bank.bankingsystem.dto.request.WithdrawRequest;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.exception.AccountNotActiveException;
import com.bank.bankingsystem.exception.InsufficientBalanceException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.Transaction;
import com.bank.bankingsystem.model.User;
import com.bank.bankingsystem.model.enums.*;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.repository.TransactionRepository;
import com.bank.bankingsystem.security.AuthenticatedUser;
import com.bank.bankingsystem.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private TransactionRepository transactionRepository;
    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        customerRepository = mock(CustomerRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        service = new TransactionServiceImpl(accountRepository, customerRepository, transactionRepository);
        setCurrentUser(1L, "alice", Role.CUSTOMER, UserStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------- DEPOSIT --------

    @Test
    void deposit_success() {
        Account account = activeAccount(100L, 10L, "1000000001", "100.0000");
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(10L, 1L)));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(account));
        when(accountRepository.credit(eq(100L), eq(new BigDecimal("50.00")))).thenReturn(1);

        Account after = activeAccount(100L, 10L, "1000000001", "150.0000");
        when(accountRepository.findById(100L))
                .thenReturn(Optional.of(account))
                .thenReturn(Optional.of(after));

        Transaction savedTx = new Transaction();
        savedTx.setId(500L);
        savedTx.setAccountId(100L);
        savedTx.setAmount(new BigDecimal("50.00"));
        savedTx.setBalanceBefore(new BigDecimal("100.0000"));
        savedTx.setBalanceAfter(new BigDecimal("150.0000"));
        savedTx.setTransactionType(TransactionType.DEPOSIT);
        savedTx.setReferenceNumber("TXN-1");
        when(transactionRepository.insert(any(Transaction.class))).thenReturn(500L);
        when(transactionRepository.findById(500L)).thenReturn(Optional.of(savedTx));

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("50.00"));

        TransactionResponse resp = service.deposit(100L, req);

        assertThat(resp.getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(resp.getAmount()).isEqualByComparingTo("50.00");
        assertThat(resp.getBalanceAfter()).isEqualByComparingTo("150.0000");
        verify(accountRepository).credit(100L, new BigDecimal("50.00"));
        verify(transactionRepository).insert(any(Transaction.class));
    }

    // -------- WITHDRAW --------

    @Test
    void withdraw_success() {
        Account account = activeAccount(100L, 10L, "1000000001", "200.0000");
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(10L, 1L)));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(account));
        when(accountRepository.debit(eq(100L), eq(new BigDecimal("50.00")))).thenReturn(1);

        Account after = activeAccount(100L, 10L, "1000000001", "150.0000");
        when(accountRepository.findById(100L))
                .thenReturn(Optional.of(account))
                .thenReturn(Optional.of(after));

        Transaction savedTx = new Transaction();
        savedTx.setId(501L);
        savedTx.setAccountId(100L);
        savedTx.setAmount(new BigDecimal("50.00"));
        savedTx.setBalanceBefore(new BigDecimal("200.0000"));
        savedTx.setBalanceAfter(new BigDecimal("150.0000"));
        savedTx.setTransactionType(TransactionType.WITHDRAWAL);
        savedTx.setReferenceNumber("TXN-2");
        when(transactionRepository.insert(any(Transaction.class))).thenReturn(501L);
        when(transactionRepository.findById(501L)).thenReturn(Optional.of(savedTx));

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("50.00"));

        TransactionResponse resp = service.withdraw(100L, req);

        assertThat(resp.getTransactionType()).isEqualTo("WITHDRAWAL");
        assertThat(resp.getBalanceAfter()).isEqualByComparingTo("150.0000");
    }

    @Test
    void withdraw_failsOnInsufficientBalance() {
        Account account = activeAccount(100L, 10L, "1000000001", "30.0000");
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(10L, 1L)));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(account));

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.withdraw(100L, req))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        verify(accountRepository, never()).debit(anyLong(), any());
        verify(transactionRepository, never()).insert(any());
    }

    @Test
    void withdraw_failsWhenAccountFrozen() {
        Account frozen = activeAccount(100L, 10L, "1000000001", "500.0000");
        frozen.setStatus(AccountStatus.FROZEN);

        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(10L, 1L)));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(frozen));

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.withdraw(100L, req))
                .isInstanceOf(AccountNotActiveException.class);

        verify(accountRepository, never()).debit(anyLong(), any());
    }

    // -------- helpers --------

    private static Account activeAccount(Long id, Long customerId, String number, String balance) {
        Account a = new Account();
        a.setId(id);
        a.setCustomerId(customerId);
        a.setAccountNumber(number);
        a.setAccountType(AccountType.SAVINGS);
        a.setBalance(new BigDecimal(balance));
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }

    private static Customer customer(Long id, Long userId) {
        Customer c = new Customer();
        c.setId(id);
        c.setUserId(userId);
        return c;
    }

    private static void setCurrentUser(Long id, String username, Role role, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPasswordHash("x");
        u.setRole(role);
        u.setStatus(status);
        AuthenticatedUser principal = new AuthenticatedUser(u);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}