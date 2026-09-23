package com.bank.bankingsystem.model;

import com.bank.bankingsystem.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

    private Long id;
    private String referenceNumber;
    private Long accountId;
    private Long relatedAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(Long id, String referenceNumber, Long accountId, Long relatedAccountId,
                       TransactionType transactionType, BigDecimal amount,
                       BigDecimal balanceBefore, BigDecimal balanceAfter,
                       String description, LocalDateTime createdAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.accountId = accountId;
        this.relatedAccountId = relatedAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getRelatedAccountId() { return relatedAccountId; }
    public void setRelatedAccountId(Long relatedAccountId) { this.relatedAccountId = relatedAccountId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
