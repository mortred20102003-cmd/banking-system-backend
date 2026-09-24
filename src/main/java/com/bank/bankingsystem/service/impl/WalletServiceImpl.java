package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.response.WalletResponse;
import com.bank.bankingsystem.exception.BusinessRuleException;
import com.bank.bankingsystem.exception.InsufficientBalanceException;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.Transaction;
import com.bank.bankingsystem.model.Wallet;
import com.bank.bankingsystem.model.WalletTransaction;
import com.bank.bankingsystem.model.enums.AccountStatus;
import com.bank.bankingsystem.model.enums.TransactionType;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.repository.TransactionRepository;
import com.bank.bankingsystem.repository.WalletRepository;
import com.bank.bankingsystem.service.WalletService;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private static final SecureRandom RNG = new SecureRandom();

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             AccountRepository accountRepository,
                             CustomerRepository customerRepository,
                             TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public WalletResponse getOrCreateMyWallet() {
        Customer me = currentCustomer();
        Wallet w = walletRepository.findByCustomerId(me.getId()).orElseGet(() -> {
            Wallet nw = new Wallet();
            nw.setCustomerId(me.getId());
            nw.setWalletNumber(generateWalletNumber());
            Long id = walletRepository.insertWallet(nw);
            return walletRepository.findByCustomerId(me.getId()).orElseThrow();
        });
        return WalletResponse.from(w);
    }

    @Override
    @Transactional
    public WalletResponse cashIn(Long accountId, BigDecimal amount, String description) {
        Customer me = currentCustomer();
        Account account = mustOwnActiveAccount(me.getId(), accountId);

        Wallet wallet = ensureWallet(me);

        // Debit bank account atomically
        int debit = accountRepository.debit(account.getId(), amount);
        if (debit == 0) throw new InsufficientBalanceException("Insufficient bank balance");

        BigDecimal before = wallet.getBalance();
        int credit = walletRepository.credit(wallet.getId(), amount);
        if (credit == 0) throw new BusinessRuleException("Wallet credit failed");

        Wallet after = walletRepository.findByCustomerId(me.getId()).orElseThrow();

        // Bank-side ledger (WITHDRAWAL)
        String ref = "WCI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        transactionRepository.insert(new Transaction(
                null, ref, account.getId(), null, TransactionType.WITHDRAWAL,
                amount, account.getBalance(), account.getBalance().subtract(amount),
                "GCash cash-in → wallet " + wallet.getWalletNumber(), null));

        // Wallet-side ledger
        WalletTransaction wtx = new WalletTransaction();
        wtx.setWalletId(wallet.getId());
        wtx.setLinkedAccountId(account.getId());
        wtx.setType("CASH_IN");
        wtx.setAmount(amount);
        wtx.setBalanceBefore(before);
        wtx.setBalanceAfter(after.getBalance());
        wtx.setReferenceNumber(ref);
        wtx.setDescription(description == null || description.isBlank()
                ? "Cash-in from " + account.getAccountNumber() : description.trim());
        walletRepository.insertTx(wtx);

        return WalletResponse.from(after);
    }

    @Override
    @Transactional
    public WalletResponse cashOut(Long accountId, BigDecimal amount, String description) {
        Customer me = currentCustomer();
        Account account = mustOwnActiveAccount(me.getId(), accountId);
        Wallet wallet = ensureWallet(me);

        Wallet locked = walletRepository.findByIdForUpdate(wallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (locked.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }
        BigDecimal before = locked.getBalance();

        int debit = walletRepository.debit(locked.getId(), amount);
        if (debit == 0) throw new InsufficientBalanceException("Wallet debit failed");

        int credit = accountRepository.credit(account.getId(), amount);
        if (credit == 0) throw new BusinessRuleException("Bank credit failed — rolled back");

        Wallet after = walletRepository.findByCustomerId(me.getId()).orElseThrow();

        String ref = "WCO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        transactionRepository.insert(new Transaction(
                null, ref, account.getId(), null, TransactionType.DEPOSIT,
                amount, account.getBalance(), account.getBalance().add(amount),
                "GCash cash-out ← wallet " + wallet.getWalletNumber(), null));

        WalletTransaction wtx = new WalletTransaction();
        wtx.setWalletId(wallet.getId());
        wtx.setLinkedAccountId(account.getId());
        wtx.setType("CASH_OUT");
        wtx.setAmount(amount);
        wtx.setBalanceBefore(before);
        wtx.setBalanceAfter(after.getBalance());
        wtx.setReferenceNumber(ref);
        wtx.setDescription(description == null || description.isBlank()
                ? "Cash-out to " + account.getAccountNumber() : description.trim());
        walletRepository.insertTx(wtx);

        return WalletResponse.from(after);
    }

    @Override
    public List<WalletTransaction> myTransactions(int page, int size) {
        Customer me = currentCustomer();
        Wallet w = ensureWallet(me);
        int p = Math.max(0, page);
        int s = Math.min(Math.max(1, size), 200);
        return walletRepository.findTransactions(w.getId(), s, p * s);
    }

    // ---------------- helpers ----------------

    private Customer currentCustomer() {
        Long userId = CurrentUserUtil.currentUserId();
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile missing"));
    }

    private Account mustOwnActiveAccount(Long customerId, Long accountId) {
        Account a = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!a.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccountAccessException("You do not own this account");
        }
        if (a.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account is " + a.getStatus());
        }
        return a;
    }

    private Wallet ensureWallet(Customer me) {
        return walletRepository.findByCustomerId(me.getId()).orElseGet(() -> {
            Wallet nw = new Wallet();
            nw.setCustomerId(me.getId());
            nw.setWalletNumber(generateWalletNumber());
            walletRepository.insertWallet(nw);
            return walletRepository.findByCustomerId(me.getId()).orElseThrow();
        });
    }

    private String generateWalletNumber() {
        String num;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder("09");
            for (int i = 0; i < 9; i++) sb.append(RNG.nextInt(10));
            num = sb.toString();
            attempts++;
        } while (walletRepository.existsByNumber(num) && attempts < 20);
        return num;
    }
}
