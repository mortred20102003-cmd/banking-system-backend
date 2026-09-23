package com.bank.bankingsystem.dto.request;

import com.bank.bankingsystem.model.enums.Role;
import jakarta.validation.constraints.NotNull;

public class RoleChangeRequest {

    @NotNull(message = "Role is required")
    private Role role;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
