package com.bank.bankingsystem.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateCustomerRequest {

    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String middleName;

    @Size(max = 60)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
