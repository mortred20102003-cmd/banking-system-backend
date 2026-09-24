package com.bank.bankingsystem.dto.request;

import jakarta.validation.constraints.NotNull;

public class GenerateCardRequest {
    @NotNull(message = "Linked account is required")
    private Long accountId;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
