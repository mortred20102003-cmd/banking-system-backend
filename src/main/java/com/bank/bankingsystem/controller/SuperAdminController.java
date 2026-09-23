package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.AdjustCreditRequest;
import com.bank.bankingsystem.dto.request.AdminUpdateCustomerRequest;
import com.bank.bankingsystem.dto.request.RoleChangeRequest;
import com.bank.bankingsystem.dto.response.ApiResponse;
import com.bank.bankingsystem.dto.response.AuditLogResponse;
import com.bank.bankingsystem.dto.response.CustomerDetailResponse;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.dto.response.UserResponse;
import com.bank.bankingsystem.service.SuperAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> allUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved",
                superAdminService.listAllUsers()));
    }

    @PostMapping("/users/{userId}/promote")
    public ResponseEntity<ApiResponse<UserResponse>> promote(
            @PathVariable Long userId,
            @Valid @RequestBody RoleChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User promoted to ADMIN",
                superAdminService.promoteToAdmin(userId, request)));
    }

    @PostMapping("/users/{userId}/demote")
    public ResponseEntity<ApiResponse<UserResponse>> demote(
            @PathVariable Long userId,
            @Valid @RequestBody RoleChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User demoted to CUSTOMER",
                superAdminService.demoteToCustomer(userId, request)));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable Long userId,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("User status updated",
                superAdminService.changeUserStatus(userId, status)));
    }

    // ---------- ADJUST CREDIT (audited deposit) ----------
    @PostMapping("/accounts/{accountId}/adjust-credit")
    public ResponseEntity<ApiResponse<TransactionResponse>> adjustCredit(
            @PathVariable Long accountId,
            @Valid @RequestBody AdjustCreditRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Adjustment credit applied",
                superAdminService.adjustCredit(accountId, request.getAmount(), request.getReason())));
    }

    // ---------- CUSTOMER DETAIL ----------
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> customerDetail(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved",
                superAdminService.getCustomerDetail(customerId)));
    }

    // ---------- CUSTOMER EDIT ----------
    @PutMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody AdminUpdateCustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated",
                superAdminService.updateCustomer(customerId, request)));
    }

    // ---------- AUDIT LOG ----------
    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<java.util.List<AuditLogResponse>>> auditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success("Audit log retrieved",
                superAdminService.listAuditLog(page, size)));
    }
}
