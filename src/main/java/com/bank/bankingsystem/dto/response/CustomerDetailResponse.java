package com.bank.bankingsystem.dto.response;

import java.util.List;

public class CustomerDetailResponse {

    private UserResponse user;
    private CustomerResponse customer;
    private List<AccountResponse> accounts;

    public CustomerDetailResponse() { }

    public CustomerDetailResponse(UserResponse user, CustomerResponse customer, List<AccountResponse> accounts) {
        this.user = user;
        this.customer = customer;
        this.accounts = accounts;
    }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
    public CustomerResponse getCustomer() { return customer; }
    public void setCustomer(CustomerResponse customer) { this.customer = customer; }
    public List<AccountResponse> getAccounts() { return accounts; }
    public void setAccounts(List<AccountResponse> accounts) { this.accounts = accounts; }
}
