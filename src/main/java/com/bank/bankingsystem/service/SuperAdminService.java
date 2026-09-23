package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.AdminUpdateCustomerRequest;
import com.bank.bankingsystem.dto.request.RoleChangeRequest;
import com.bank.bankingsystem.dto.response.AuditLogResponse;
import com.bank.bankingsystem.dto.response.CustomerDetailResponse;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.dto.response.UserResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SuperAdminService {

    List<UserResponse> listAllUsers();

    UserResponse promoteToAdmin(Long userId, RoleChangeRequest request);

    UserResponse demoteToCustomer(Long userId, RoleChangeRequest request);

    UserResponse changeUserStatus(Long userId, String status);

    TransactionResponse adjustCredit(Long accountId, BigDecimal amount, String reason);

    CustomerDetailResponse getCustomerDetail(Long customerId);

    CustomerDetailResponse updateCustomer(Long customerId, AdminUpdateCustomerRequest request);

    List<AuditLogResponse> listAuditLog(int page, int size);
}
