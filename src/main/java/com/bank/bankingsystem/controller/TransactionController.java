package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.response.ApiResponse;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** Look up a transfer pair (or a single deposit/withdrawal) by reference. */
    @GetMapping("/{referenceNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getByReference(
            @PathVariable String referenceNumber) {
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved",
                transactionService.getByReferenceNumber(referenceNumber)));
    }

    /** Legacy numeric lookup (deposits and withdrawals only). */
    @GetMapping("/id/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved",
                transactionService.getById(transactionId)));
    }
}
