package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.CreateAccountRequest;
import com.bank.bankingsystem.dto.request.DepositRequest;
import com.bank.bankingsystem.dto.request.WithdrawRequest;
import com.bank.bankingsystem.dto.response.*;
import com.bank.bankingsystem.service.AccountService;
import com.bank.bankingsystem.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService,
                             TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    // ---------- create ----------
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created",
                        accountService.createAccount(request)));
    }

    // ---------- list my accounts ----------
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> listMine() {
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved",
                accountService.listMyAccounts()));
    }

    // ---------- get by id ----------
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getById(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Account retrieved",
                accountService.getMyAccountById(accountId)));
    }

    // ---------- get by account number ----------
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getByNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Account retrieved",
                accountService.getMyAccountByNumber(accountNumber)));
    }

    // ---------- balance ----------
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> balance(
            @PathVariable Long accountId) {
        BigDecimal bal = accountService.getBalance(accountId);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved",
                Map.of("accountId", accountId, "balance", bal)));
    }

    // ---------- deposit ----------
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Deposit successful",
                transactionService.deposit(accountId, request)));
    }

    // ---------- withdraw ----------
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful",
                transactionService.withdraw(accountId, request)));
    }

    // ---------- transaction history for this account ----------
    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> history(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("History retrieved",
                transactionService.getMyTransactionHistory(accountId, page, size)));
    }
}