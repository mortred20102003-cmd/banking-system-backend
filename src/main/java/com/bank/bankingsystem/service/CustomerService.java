package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.request.UpdateCustomerRequest;
import com.bank.bankingsystem.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse getMyProfile();

    CustomerResponse updateMyProfile(UpdateCustomerRequest request);

    CustomerResponse getById(Long customerId);        // ADMIN+

    List<CustomerResponse> getAll();                  // ADMIN+
}