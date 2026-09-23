package com.bank.bankingsystem.dto.response;

public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresInMs;
    private Long userId;
    private String username;
    private String email;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, long expiresInMs,
                        Long userId, String username, String email, String role) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
