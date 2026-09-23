package com.bank.bankingsystem.dto.response;

import com.bank.bankingsystem.model.User;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.email = u.getEmail();
        r.role = u.getRole().name();
        r.status = u.getStatus().name();
        r.createdAt = u.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
