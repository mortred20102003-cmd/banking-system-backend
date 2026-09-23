package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.request.AccountStatusChangeRequest;
import com.bank.bankingsystem.dto.request.CreateAccountRequest;
import com.bank.bankingsystem.dto.response.AccountResponse;
import com.bank.bankingsystem.exception.AccountNotActiveException;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.enums.AccountStatus;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.service.AccountService;
import com.bank.bankingsystem.util.AccountNumberGenerator;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountServiceImpl(AccountRepository accountRepository,
                              CustomerRepository customerRepository,
                              AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest req) {
        Long userId = CurrentUserUtil.currentUserId();

        Customer owner = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for current user"));

        Account a = new Account();
        a.setCustomerId(owner.getId());
        a.setAccountNumber(accountNumberGenerator.generate());
        a.setAccountType(req.getAccountType());
        a.setBalance(BigDecimal.ZERO.setScale(4));
        a.setStatus(AccountStatus.ACTIVE);

        Long id = accountRepository.insert(a);
        Account saved = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Account vanished after insert"));

        log.info("Created account id={} number={} for customerId={}",
                saved.getId(), saved.getAccountNumber(), owner.getId());
        return AccountResponse.from(saved);
    }

    @Override
    public List<AccountResponse> listMyAccounts() {
        Customer me = currentCustomer();
        return accountRepository.findByCustomerId(me.getId()).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Override
    public AccountResponse getMyAccountById(Long accountId) {
        Account a = loadAndAuthorize(accountId);
        return AccountResponse.from(a);
    }

    @Override
    public AccountResponse getMyAccountByNumber(String accountNumber) {
        Account a = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber));
        assertOwnership(a);
        return AccountResponse.from(a);
    }

    @Override
    public BigDecimal getBalance(Long accountId) {
        Account a = loadAndAuthorize(accountId);
        return a.getBalance();
    }

    @Override
    public List<AccountResponse> listAllAccounts() {
        if (!CurrentUserUtil.isAdminOrAbove()) {
            throw new UnauthorizedAccountAccessException(
                    "Only ADMIN or SUPER_ADMIN can list all accounts");
        }
        return accountRepository.findAllWithOwner().stream()
                .map(r -> AccountResponse.from(r.account(), r.customerName()))
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse changeStatus(Long accountId, AccountStatusChangeRequest req) {
        if (!CurrentUserUtil.isAdminOrAbove()) {
            throw new UnauthorizedAccountAccessException(
                    "Only ADMIN or SUPER_ADMIN can change account status");
        }
        Account a = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: id=" + accountId));

        int rows = accountRepository.updateStatus(accountId, req.getStatus());
        if (rows == 0) {
            throw new ResourceNotFoundException("Account status update failed");
        }

        Account updated = accountRepository.findById(accountId).orElseThrow();
        log.info("Account id={} status changed {} -> {} by userId={}",
                accountId, a.getStatus(), updated.getStatus(), CurrentUserUtil.currentUserId());
        return AccountResponse.from(updated);
    }

    // ---------- helpers ----------

    private Account loadAndAuthorize(Long accountId) {
        Account a = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: id=" + accountId));
        assertOwnership(a);
        return a;
    }

    private void assertOwnership(Account a) {
        if (CurrentUserUtil.isAdminOrAbove()) return; // admin/superadmin can view any
        Customer me = currentCustomer();
        if (!a.getCustomerId().equals(me.getId())) {
            throw new UnauthorizedAccountAccessException(
                    "You do not own this account");
        }
    }

    private Customer currentCustomer() {
        Long userId = CurrentUserUtil.currentUserId();
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for current user"));
    }

    @SuppressWarnings("unused")
    private void assertActive(Account a) {
        if (a.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account is " + a.getStatus().name().toLowerCase() +
                    " and cannot be used for this operation");
        }
    }
}
