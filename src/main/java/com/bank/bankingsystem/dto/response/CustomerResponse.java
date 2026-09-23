package com.bank.bankingsystem.dto.response;

import com.bank.bankingsystem.model.Customer;

import java.time.LocalDateTime;

public class CustomerResponse {

    private Long id;
    private Long userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.id = c.getId();
        r.userId = c.getUserId();
        r.firstName = c.getFirstName();
        r.middleName = c.getMiddleName();
        r.lastName = c.getLastName();
        r.phone = c.getPhone();
        r.address = c.getAddress();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
