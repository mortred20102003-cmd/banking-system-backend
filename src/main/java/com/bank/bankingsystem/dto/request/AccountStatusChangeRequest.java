package com.bank.bankingsystem.dto.request;

import com.bank.bankingsystem.model.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class AccountStatusChangeRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
