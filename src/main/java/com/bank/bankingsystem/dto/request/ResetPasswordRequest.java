package com.bank.bankingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Size(min = 16, max = 128)
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
