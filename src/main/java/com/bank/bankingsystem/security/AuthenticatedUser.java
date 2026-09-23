package com.bank.bankingsystem.security;

import com.bank.bankingsystem.model.User;
import com.bank.bankingsystem.model.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation that carries our domain fields
 * (userId, email, role, status) alongside Spring Security's expected contract.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final UserStatus status;

    public AuthenticatedUser(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();
        this.status = user.getStatus();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public UserStatus getStatus() { return status; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security convention: authorities are "ROLE_<NAME>"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return status != UserStatus.LOCKED; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
