package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.response.WalletResponse;
import com.bank.bankingsystem.model.WalletTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletResponse getOrCreateMyWallet();
    WalletResponse cashIn(Long accountId, BigDecimal amount, String description);   // bank -> wallet
    WalletResponse cashOut(Long accountId, BigDecimal amount, String description);  // wallet -> bank
    List<WalletTransaction> myTransactions(int page, int size);
}
