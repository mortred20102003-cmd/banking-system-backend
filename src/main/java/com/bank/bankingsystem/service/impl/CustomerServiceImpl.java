package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.request.UpdateCustomerRequest;
import com.bank.bankingsystem.dto.response.CustomerResponse;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.service.CustomerService;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public CustomerResponse getMyProfile() {
        Long userId = CurrentUserUtil.currentUserId();
        Customer c = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for current user"));
        return CustomerResponse.from(c);
    }

    @Override
    @Transactional
    public CustomerResponse updateMyProfile(UpdateCustomerRequest req) {
        Long userId = CurrentUserUtil.currentUserId();
        Customer c = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for current user"));

        // COALESCE in the repository means null fields are left untouched.
        c.setFirstName(trimOrNull(req.getFirstName()));
        c.setMiddleName(trimOrNull(req.getMiddleName()));
        c.setLastName(trimOrNull(req.getLastName()));
        c.setPhone(trimOrNull(req.getPhone()));
        c.setAddress(trimOrNull(req.getAddress()));

        int rows = customerRepository.update(c);
        if (rows == 0) {
            throw new ResourceNotFoundException("Customer update failed — not found");
        }
        return CustomerResponse.from(customerRepository.findById(c.getId()).orElseThrow());
    }

    @Override
    public CustomerResponse getById(Long customerId) {
        if (!CurrentUserUtil.isAdminOrAbove()) {
            throw new UnauthorizedAccountAccessException(
                    "Only ADMIN or SUPER_ADMIN can view other customers");
        }
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found: id=" + customerId));
        return CustomerResponse.from(c);
    }

    @Override
    public List<CustomerResponse> getAll() {
        if (!CurrentUserUtil.isAdminOrAbove()) {
            throw new UnauthorizedAccountAccessException(
                    "Only ADMIN or SUPER_ADMIN can list all customers");
        }
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}