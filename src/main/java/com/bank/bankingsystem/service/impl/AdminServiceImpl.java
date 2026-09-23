package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.response.AccountResponse;
import com.bank.bankingsystem.dto.response.CustomerResponse;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.dto.response.UserResponse;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.repository.TransactionRepository;
import com.bank.bankingsystem.repository.UserRepository;
import com.bank.bankingsystem.service.AdminService;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private static final int MAX_PAGE_SIZE = 200;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AdminServiceImpl(UserRepository userRepository,
                            CustomerRepository customerRepository,
                            AccountRepository accountRepository,
                            TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<UserResponse> listAllUsers() {
        requireAdmin();
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    public List<CustomerResponse> listAllCustomers() {
        requireAdmin();
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Override
    public List<AccountResponse> listAllAccounts() {
        requireAdmin();
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Override
    public List<TransactionResponse> listTransactionsForAccount(Long accountId, int page, int size) {
        requireAdmin();
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: id=" + accountId));
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return transactionRepository.findByAccountIdEnriched(accountId, safeSize, safePage * safeSize)
                .stream()
                .map(r -> TransactionResponse.from(
                        r.transaction(),
                        r.accountNumber(),
                        r.accountOwnerName(),
                        r.relatedAccountNumber(),
                        r.relatedAccountOwnerName()))
                .toList();
    }

    private void requireAdmin() {
        if (!CurrentUserUtil.isAdminOrAbove()) {
            throw new UnauthorizedAccountAccessException(
                    "ADMIN or SUPER_ADMIN role required");
        }
    }
}
