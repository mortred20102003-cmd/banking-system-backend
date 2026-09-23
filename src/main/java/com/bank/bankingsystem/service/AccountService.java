package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.AccountStatusChangeRequest;
import com.bank.bankingsystem.dto.request.CreateAccountRequest;
import com.bank.bankingsystem.dto.response.AccountResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    List<AccountResponse> listMyAccounts();

    AccountResponse getMyAccountById(Long accountId);

    AccountResponse getMyAccountByNumber(String accountNumber);

    BigDecimal getBalance(Long accountId);

    // ADMIN+
    List<AccountResponse> listAllAccounts();

    AccountResponse changeStatus(Long accountId, AccountStatusChangeRequest request);
}
