package com.bank.bankingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    @Size(max = 20)
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    @Size(max = 20)
    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true,
                message = "Transfer amount must be greater than zero")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
