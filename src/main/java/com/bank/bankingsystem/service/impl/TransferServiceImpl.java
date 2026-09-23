package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.request.TransferRequest;
import com.bank.bankingsystem.dto.response.TransferResult;
import com.bank.bankingsystem.exception.AccountNotActiveException;
import com.bank.bankingsystem.exception.BusinessRuleException;
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
import com.bank.bankingsystem.service.TransferService;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Atomic account-to-account transfer.
 *
 * Concurrency strategy:
 *  - Runs entirely inside ONE @Transactional method.
 *  - Locks both account rows with SELECT ... FOR UPDATE, in a deterministic
 *    order (ascending account id) to avoid deadlocks between concurrent transfers
 *    in opposite directions.
 *  - Uses conditional debit (balance >= amount) so the DB itself refuses
 *    overdrafts even if the lock is bypassed.
 *  - Writes two ledger rows (TRANSFER_OUT on source, TRANSFER_IN on destination).
 *  - Any failure throws → Spring rolls back the whole method atomically.
 */
@Service
public class TransferServiceImpl implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public TransferServiceImpl(AccountRepository accountRepository,
                               CustomerRepository customerRepository,
                               TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransferResult transfer(TransferRequest req) {
        String srcNumber = req.getSourceAccountNumber().trim();
        String dstNumber = req.getDestinationAccountNumber().trim();
        BigDecimal amount = req.getAmount();

        // ---------- 1. Structural validation ----------
        if (srcNumber.equals(dstNumber)) {
            throw new BusinessRuleException("Cannot transfer to the same account");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Transfer amount must be greater than zero");
        }

        // ---------- 2. Resolve accounts, then authorize caller ----------
        Account srcLookup = accountRepository.findByAccountNumber(srcNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source account not found: " + srcNumber));
        Account dstLookup = accountRepository.findByAccountNumber(dstNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination account not found: " + dstNumber));

        assertCanSendFrom(srcLookup);
        assertActive(srcLookup);
        assertActive(dstLookup);

        // ---------- 3. Lock in deterministic order (ascending id) ----------
        Long lowId  = Math.min(srcLookup.getId(), dstLookup.getId());
        Long highId = Math.max(srcLookup.getId(), dstLookup.getId());

        Account lockedLow = accountRepository.findByIdForUpdate(lowId)
                .orElseThrow(() -> new ResourceNotFoundException("Account vanished during lock"));
        Account lockedHigh = accountRepository.findByIdForUpdate(highId)
                .orElseThrow(() -> new ResourceNotFoundException("Account vanished during lock"));

        Account source = lockedLow.getId().equals(srcLookup.getId()) ? lockedLow : lockedHigh;
        Account destination = lockedLow.getId().equals(dstLookup.getId()) ? lockedLow : lockedHigh;

        // ---------- 4. Balance check (fast fail before mutations) ----------
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + source.getBalance());
        }

        BigDecimal sourceBefore = source.getBalance();
        BigDecimal destinationBefore = destination.getBalance();

        // ---------- 5. Debit source (conditional, atomic) ----------
        int debitRows = accountRepository.debit(source.getId(), amount);
        if (debitRows == 0) {
            throw new InsufficientBalanceException(
                    "Transfer failed: insufficient balance or source not active");
        }

        // ---------- 6. Credit destination ----------
        int creditRows = accountRepository.credit(destination.getId(), amount);
        if (creditRows == 0) {
            // Force rollback — the @Transactional wrapper will undo the debit
            throw new AccountNotActiveException(
                    "Transfer failed: destination not active. Rolling back.");
        }

        // ---------- 7. Reload balances for ledger rows ----------
        Account sourceAfter = accountRepository.findById(source.getId()).orElseThrow();
        Account destAfter = accountRepository.findById(destination.getId()).orElseThrow();

        String reference = generateReference();
        String description = (req.getDescription() == null || req.getDescription().isBlank())
                ? "Transfer to " + destination.getAccountNumber()
                : req.getDescription().trim();

        // ---------- 8. Ledger: TRANSFER_OUT ----------
        transactionRepository.insert(new Transaction(
                null,
                reference,
                source.getId(),
                destination.getId(),
                TransactionType.TRANSFER_OUT,
                amount,
                sourceBefore,
                sourceAfter.getBalance(),
                description,
                null
        ));

        // ---------- 9. Ledger: TRANSFER_IN ----------
        transactionRepository.insert(new Transaction(
                null,
                reference,
                destination.getId(),
                source.getId(),
                TransactionType.TRANSFER_IN,
                amount,
                destinationBefore,
                destAfter.getBalance(),
                "Transfer from " + source.getAccountNumber(),
                null
        ));

        log.info("Transfer: ref={} src={} dst={} amount={}",
                reference, source.getAccountNumber(), destination.getAccountNumber(), amount);

        return new TransferResult(
                reference,
                source.getAccountNumber(),
                destination.getAccountNumber(),
                amount,
                sourceAfter.getBalance(),
                destAfter.getBalance()
        );
    }

    // ---------- helpers ----------

    private void assertCanSendFrom(Account source) {
        if (CurrentUserUtil.isAdminOrAbove()) return;

        Long userId = CurrentUserUtil.currentUserId();
        Customer me = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for current user"));
        if (!source.getCustomerId().equals(me.getId())) {
            throw new UnauthorizedAccountAccessException(
                    "You can only transfer from accounts you own");
        }
    }

    private void assertActive(Account a) {
        if (a.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account " + a.getAccountNumber() + " is " + a.getStatus().name().toLowerCase());
        }
    }

    private String generateReference() {
        return "TRF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
