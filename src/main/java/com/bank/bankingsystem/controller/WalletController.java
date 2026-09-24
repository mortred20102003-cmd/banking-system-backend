package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.WalletTransferRequest;
import com.bank.bankingsystem.dto.response.ApiResponse;
import com.bank.bankingsystem.dto.response.WalletResponse;
import com.bank.bankingsystem.model.WalletTransaction;
import com.bank.bankingsystem.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) { this.walletService = walletService; }

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> myWallet() {
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved",
                walletService.getOrCreateMyWallet()));
    }

    @PostMapping("/cash-in")
    public ResponseEntity<ApiResponse<WalletResponse>> cashIn(
            @Valid @RequestBody WalletTransferRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Cash-in successful",
                walletService.cashIn(req.getAccountId(), req.getAmount(), req.getDescription())));
    }

    @PostMapping("/cash-out")
    public ResponseEntity<ApiResponse<WalletResponse>> cashOut(
            @Valid @RequestBody WalletTransferRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Cash-out successful",
                walletService.cashOut(req.getAccountId(), req.getAmount(), req.getDescription())));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransaction>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success("Wallet history retrieved",
                walletService.myTransactions(page, size)));
    }
}
