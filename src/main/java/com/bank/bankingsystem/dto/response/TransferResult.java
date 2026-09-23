package com.bank.bankingsystem.dto.response;

import java.math.BigDecimal;

public class TransferResult {

    private String referenceNumber;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destinationBalanceAfter;

    public TransferResult() {
    }

    public TransferResult(String referenceNumber, String sourceAccountNumber,
                          String destinationAccountNumber, BigDecimal amount,
                          BigDecimal sourceBalanceAfter, BigDecimal destinationBalanceAfter) {
        this.referenceNumber = referenceNumber;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.sourceBalanceAfter = sourceBalanceAfter;
        this.destinationBalanceAfter = destinationBalanceAfter;
    }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getSourceBalanceAfter() { return sourceBalanceAfter; }
    public void setSourceBalanceAfter(BigDecimal sourceBalanceAfter) { this.sourceBalanceAfter = sourceBalanceAfter; }

    public BigDecimal getDestinationBalanceAfter() { return destinationBalanceAfter; }
    public void setDestinationBalanceAfter(BigDecimal destinationBalanceAfter) { this.destinationBalanceAfter = destinationBalanceAfter; }
}