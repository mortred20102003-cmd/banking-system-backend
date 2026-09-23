package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.response.AccountResponse;
import com.bank.bankingsystem.dto.response.CustomerResponse;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.dto.response.UserResponse;

import java.util.List;

public interface AdminService {

    List<UserResponse> listAllUsers();

    List<CustomerResponse> listAllCustomers();

    List<AccountResponse> listAllAccounts();

    List<TransactionResponse> listTransactionsForAccount(Long accountId, int page, int size);
}
