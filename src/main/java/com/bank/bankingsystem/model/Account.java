package com.bank.bankingsystem.model;

import com.bank.bankingsystem.model.enums.AccountStatus;
import com.bank.bankingsystem.model.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {

    private Long id;
    private Long customerId;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {
    }

    public Account(Long id, Long customerId, String accountNumber,
                   AccountType accountType, BigDecimal balance,
                   AccountStatus status,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
