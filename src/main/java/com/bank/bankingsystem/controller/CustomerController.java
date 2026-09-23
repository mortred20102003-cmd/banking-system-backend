package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.UpdateCustomerRequest;
import com.bank.bankingsystem.dto.response.ApiResponse;
import com.bank.bankingsystem.dto.response.CustomerResponse;
import com.bank.bankingsystem.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved",
                customerService.getMyProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateMyProfile(
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                customerService.updateMyProfile(request)));
    }
}