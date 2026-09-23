package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.TransferRequest;
import com.bank.bankingsystem.dto.response.ApiResponse;
import com.bank.bankingsystem.dto.response.TransferResult;
import com.bank.bankingsystem.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResult>> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransferResult result = transferService.transfer(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Transfer completed successfully", result));
    }
}