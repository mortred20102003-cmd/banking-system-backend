package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.ForgotPasswordRequest;
import com.bank.bankingsystem.dto.request.LoginRequest;
import com.bank.bankingsystem.dto.request.ResetPasswordRequest;
import com.bank.bankingsystem.dto.request.RegisterRequest;
import com.bank.bankingsystem.dto.response.ApiResponse;
import com.bank.bankingsystem.dto.response.AuthResponse;
import com.bank.bankingsystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", data));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If that email exists, a reset token has been issued. Check the server console."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successful. You can now log in with your new password."));
    }
}
