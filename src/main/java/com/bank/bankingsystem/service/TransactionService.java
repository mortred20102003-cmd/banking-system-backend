package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.DepositRequest;
import com.bank.bankingsystem.dto.request.WithdrawRequest;
import com.bank.bankingsystem.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse deposit(Long accountId, DepositRequest request);

    TransactionResponse withdraw(Long accountId, WithdrawRequest request);

    List<TransactionResponse> getMyTransactionHistory(Long accountId, int page, int size);

    TransactionResponse getById(Long transactionId);
    java.util.List<TransactionResponse> getByReferenceNumber(String referenceNumber);
}
