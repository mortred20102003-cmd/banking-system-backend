package com.bank.bankingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class WithdrawRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true,
                message = "Withdrawal amount must be greater than zero")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
