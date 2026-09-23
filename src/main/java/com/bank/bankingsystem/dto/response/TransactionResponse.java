package com.bank.bankingsystem.dto.response;

import com.bank.bankingsystem.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String referenceNumber;
    private Long accountId;
    private String accountNumber;
    private String accountOwnerName;
    private Long relatedAccountId;
    private String relatedAccountNumber;
    private String relatedAccountOwnerName;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction t) {
        return from(t, null, null, null, null);
    }

    public static TransactionResponse from(Transaction t,
                                           String accountNumber,
                                           String accountOwnerName,
                                           String relatedAccountNumber,
                                           String relatedAccountOwnerName) {
        TransactionResponse r = new TransactionResponse();
        r.id = t.getId();
        r.referenceNumber = t.getReferenceNumber();
        r.accountId = t.getAccountId();
        r.accountNumber = accountNumber;
        r.accountOwnerName = accountOwnerName;
        r.relatedAccountId = t.getRelatedAccountId();
        r.relatedAccountNumber = relatedAccountNumber;
        r.relatedAccountOwnerName = relatedAccountOwnerName;
        r.transactionType = t.getTransactionType().name();
        r.amount = t.getAmount();
        r.balanceBefore = t.getBalanceBefore();
        r.balanceAfter = t.getBalanceAfter();
        r.description = t.getDescription();
        r.createdAt = t.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public Long getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountOwnerName() { return accountOwnerName; }
    public Long getRelatedAccountId() { return relatedAccountId; }
    public String getRelatedAccountNumber() { return relatedAccountNumber; }
    public String getRelatedAccountOwnerName() { return relatedAccountOwnerName; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
