package com.bank.bankingsystem.dto.response;

import com.bank.bankingsystem.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {
    private Long id;
    private String walletNumber;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;

    public static WalletResponse from(Wallet w) {
        WalletResponse r = new WalletResponse();
        r.id = w.getId();
        r.walletNumber = w.getWalletNumber();
        r.balance = w.getBalance();
        r.status = w.getStatus();
        r.createdAt = w.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getWalletNumber() { return walletNumber; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
