package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.TransferRequest;
import com.bank.bankingsystem.dto.response.TransferResult;

public interface TransferService {

    TransferResult transfer(TransferRequest request);
}