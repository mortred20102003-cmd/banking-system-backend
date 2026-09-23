package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.AccountStatusChangeRequest;
import com.bank.bankingsystem.dto.response.*;
import com.bank.bankingsystem.service.AccountService;
import com.bank.bankingsystem.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AccountService accountService;

    public AdminController(AdminService adminService, AccountService accountService) {
        this.adminService = adminService;
        this.accountService = accountService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> allUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved",
                adminService.listAllUsers()));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> allCustomers() {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved",
                adminService.listAllCustomers()));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> allAccounts() {
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved",
                adminService.listAllAccounts()));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> accountHistory(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("History retrieved",
                adminService.listTransactionsForAccount(accountId, page, size)));
    }

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> changeAccountStatus(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account status updated",
                accountService.changeStatus(accountId, request)));
    }
}