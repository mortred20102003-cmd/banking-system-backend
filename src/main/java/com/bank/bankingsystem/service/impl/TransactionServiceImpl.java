package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.request.DepositRequest;
import com.bank.bankingsystem.dto.request.WithdrawRequest;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.exception.AccountNotActiveException;
import com.bank.bankingsystem.exception.InsufficientBalanceException;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.Transaction;
import com.bank.bankingsystem.model.enums.AccountStatus;
import com.bank.bankingsystem.model.enums.TransactionType;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.repository.TransactionRepository;
import com.bank.bankingsystem.service.TransactionService;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 200;

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(AccountRepository accountRepository,
                                  CustomerRepository customerRepository,
                                  TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    // =========================================================
    // DEPOSIT
    // =========================================================
    @Override
    @Transactional
    public TransactionResponse deposit(Long accountId, DepositRequest req) {
        Account account = loadOwnedActive(accountId);
        BigDecimal amount = req.getAmount();

        // Row-lock the account so concurrent ops don't interleave.
        Account locked = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (locked.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is " + locked.getStatus());
        }

        BigDecimal before = locked.getBalance();
        int rows = accountRepository.credit(locked.getId(), amount);
        if (rows == 0) {
            throw new AccountNotActiveException("Deposit rejected: account not active");
        }

        Account after = accountRepository.findById(locked.getId()).orElseThrow();
        Transaction tx = new Transaction(
                null,
                generateReference(),
                locked.getId(),
                null,
                TransactionType.DEPOSIT,
                amount,
                before,
                after.getBalance(),
                defaultDescription(req.getDescription(), "Deposit"),
                null
        );
        Long txId = transactionRepository.insert(tx);

        log.info("Deposit: accountId={} amount={} before={} after={}",
                locked.getId(), amount, before, after.getBalance());

        return TransactionResponse.from(transactionRepository.findById(txId).orElseThrow());
    }

    // =========================================================
    // WITHDRAWAL
    // =========================================================
    @Override
    @Transactional
    public TransactionResponse withdraw(Long accountId, WithdrawRequest req) {
        Account account = loadOwnedActive(accountId);
        BigDecimal amount = req.getAmount();

        Account locked = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (locked.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is " + locked.getStatus());
        }
        if (locked.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + locked.getBalance());
        }

        BigDecimal before = locked.getBalance();

        // Atomic, conditional debit — DB guarantees balance never goes negative
        int rows = accountRepository.debit(locked.getId(), amount);
        if (rows == 0) {
            // Lost the race or account became inactive mid-flight
            throw new InsufficientBalanceException(
                    "Withdrawal failed: insufficient balance or account not active");
        }

        Account after = accountRepository.findById(locked.getId()).orElseThrow();
        Transaction tx = new Transaction(
                null,
                generateReference(),
                locked.getId(),
                null,
                TransactionType.WITHDRAWAL,
                amount,
                before,
                after.getBalance(),
                defaultDescription(req.getDescription(), "Withdrawal"),
                null
        );
        Long txId = transactionRepository.insert(tx);

        log.info("Withdrawal: accountId={} amount={} before={} after={}",
                locked.getId(), amount, before, after.getBalance());

        return TransactionResponse.from(transactionRepository.findById(txId).orElseThrow());
    }

    // =========================================================
    // HISTORY
    // =========================================================
    @Override
    public List<TransactionResponse> getMyTransactionHistory(Long accountId, int page, int size) {
        Account a = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: id=" + accountId));
        assertOwnership(a);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        return transactionRepository.findByAccountId(accountId, safeSize, safePage * safeSize)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Override
    public TransactionResponse getById(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: id=" + transactionId));

        // Fetch the account and verify ownership (or admin)
        Account a = accountRepository.findById(tx.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        assertOwnership(a);
        return TransactionResponse.from(tx);
    }

    // =========================================================
    // helpers
    // =========================================================
    private Account loadOwnedActive(Long accountId) {
        Account a = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: id=" + accountId));
        assertOwnership(a);
        if (a.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account is " + a.getStatus().name().toLowerCase());
        }
        return a;
    }

    private void assertOwnership(Account a) {
        if (CurrentUserUtil.isAdminOrAbove()) return;
        Long userId = CurrentUserUtil.currentUserId();
        Customer me = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for current user"));
        if (!a.getCustomerId().equals(me.getId())) {
            throw new UnauthorizedAccountAccessException("You do not own this account");
        }
    }

    private String defaultDescription(String provided, String fallback) {
        return (provided == null || provided.isBlank()) ? fallback : provided.trim();
    }

    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    @Override
    public List<TransactionResponse> getByReferenceNumber(String referenceNumber) {
        List<Transaction> rows = transactionRepository.findByReferenceNumber(referenceNumber);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No transactions found for reference: " + referenceNumber);
        }
        Transaction first = rows.get(0);
        Account a = accountRepository.findById(first.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        assertOwnership(a);
        return rows.stream().map(TransactionResponse::from).toList();
    }
}
