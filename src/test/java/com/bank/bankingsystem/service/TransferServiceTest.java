package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.TransferRequest;
import com.bank.bankingsystem.dto.response.TransferResult;
import com.bank.bankingsystem.exception.BusinessRuleException;
import com.bank.bankingsystem.exception.InsufficientBalanceException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.User;
import com.bank.bankingsystem.model.enums.*;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.repository.TransactionRepository;
import com.bank.bankingsystem.security.AuthenticatedUser;
import com.bank.bankingsystem.service.impl.TransferServiceImpl;
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

class TransferServiceTest {

    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private TransactionRepository transactionRepository;
    private TransferServiceImpl service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        customerRepository = mock(CustomerRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        service = new TransferServiceImpl(accountRepository, customerRepository, transactionRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transfer_rejectsSameAccount() {
        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("1000000001");
        req.setDestinationAccountNumber("1000000001");
        req.setAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.transfer(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("same account");
    }

    @Test
    void transfer_rejectsNonPositiveAmount() {
        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("1000000001");
        req.setDestinationAccountNumber("1000000002");
        req.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.transfer(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void transfer_failsWhenCallerDoesNotOwnSource() {
        setCurrentUser(1L, "alice", Role.CUSTOMER, UserStatus.ACTIVE);

        Account src = account(1L, 10L, "1000000001", "100.0000");
        Account dst = account(2L, 20L, "1000000002", "0.0000");

        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(src));
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(dst));
        // alice is customer 999 — not the owner of src (customer 10)
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(999L, 1L)));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("1000000001");
        req.setDestinationAccountNumber("1000000002");
        req.setAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.transfer(req))
                .isInstanceOf(UnauthorizedAccountAccessException.class);

        verify(accountRepository, never()).debit(anyLong(), any());
        verify(accountRepository, never()).credit(anyLong(), any());
    }

    @Test
    void transfer_success_debitsSourceAndCreditsDestination() {
        setCurrentUser(1L, "alice", Role.CUSTOMER, UserStatus.ACTIVE);

        Account src = account(1L, 10L, "1000000001", "100.0000");
        Account dst = account(2L, 20L, "1000000002", "0.0000");

        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(src));
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(dst));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(10L, 1L)));

        // Locked reads return the same objects
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(src));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(dst));

        when(accountRepository.debit(1L, new BigDecimal("30.00"))).thenReturn(1);
        when(accountRepository.credit(2L, new BigDecimal("30.00"))).thenReturn(1);

        Account srcAfter = account(1L, 10L, "1000000001", "70.0000");
        Account dstAfter = account(2L, 20L, "1000000002", "30.0000");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(srcAfter));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(dstAfter));

        when(transactionRepository.insert(any())).thenReturn(1L);

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("1000000001");
        req.setDestinationAccountNumber("1000000002");
        req.setAmount(new BigDecimal("30.00"));
        req.setDescription("Rent");

        TransferResult result = service.transfer(req);

        assertThat(result.getSourceBalanceAfter()).isEqualByComparingTo("70.0000");
        assertThat(result.getDestinationBalanceAfter()).isEqualByComparingTo("30.0000");
        assertThat(result.getAmount()).isEqualByComparingTo("30.00");
        assertThat(result.getReferenceNumber()).startsWith("TRF-");

        verify(accountRepository).debit(1L, new BigDecimal("30.00"));
        verify(accountRepository).credit(2L, new BigDecimal("30.00"));
        // Two ledger rows
        verify(transactionRepository, times(2)).insert(any());
    }

    @Test
    void transfer_failsWhenSourceInsufficient() {
        setCurrentUser(1L, "alice", Role.CUSTOMER, UserStatus.ACTIVE);

        Account src = account(1L, 10L, "1000000001", "10.0000");
        Account dst = account(2L, 20L, "1000000002", "0.0000");
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(src));
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(dst));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(10L, 1L)));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(src));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(dst));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("1000000001");
        req.setDestinationAccountNumber("1000000002");
        req.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.transfer(req))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(accountRepository, never()).debit(anyLong(), any());
        verify(accountRepository, never()).credit(anyLong(), any());
        verify(transactionRepository, never()).insert(any());
    }

    // -------- helpers --------

    private static Account account(Long id, Long customerId, String number, String balance) {
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