package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.request.LoginRequest;
import com.bank.bankingsystem.dto.request.RegisterRequest;
import com.bank.bankingsystem.dto.response.AuthResponse;
import com.bank.bankingsystem.exception.AuthenticationException;
import com.bank.bankingsystem.exception.DuplicateResourceException;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.User;
import com.bank.bankingsystem.model.enums.Role;
import com.bank.bankingsystem.model.enums.UserStatus;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.repository.PasswordResetTokenRepository;
import com.bank.bankingsystem.repository.UserRepository;
import com.bank.bankingsystem.security.AuthenticatedUser;
import com.bank.bankingsystem.security.JwtService;
import com.bank.bankingsystem.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository resetTokenRepository;

    public AuthServiceImpl(UserRepository userRepository,
                           CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           PasswordResetTokenRepository resetTokenRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resetTokenRepository = resetTokenRepository;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String username = req.getUsername().trim();
        String email = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        Long userId = userRepository.insert(user);

        Customer c = new Customer();
        c.setUserId(userId);
        c.setFirstName(req.getFirstName().trim());
        c.setMiddleName(trimOrNull(req.getMiddleName()));
        c.setLastName(req.getLastName().trim());
        c.setPhone(trimOrNull(req.getPhone()));
        c.setAddress(trimOrNull(req.getAddress()));
        customerRepository.insert(c);

        // Reload for complete object (timestamps, id) then issue token
        User saved = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User vanished after insert"));

        log.info("Registered new user id={} username={}", userId, username);
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        String identifier = req.getUsernameOrEmail().trim();

        User user = userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("Account is " + user.getStatus().name().toLowerCase());
        }

        log.info("Login successful for user id={} username={}", user.getId(), user.getUsername());
        return buildAuthResponse(user);
    }

    // ---------- helpers ----------

    private AuthResponse buildAuthResponse(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        String token = jwtService.generateToken(principal);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // =========================================================
    // PASSWORD RESET
    // =========================================================
    private static final long RESET_TTL_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();

        // Do not leak whether the email exists.
        userRepository.findByEmail(normalized).ifPresent(user -> {
            // Invalidate any outstanding tokens for this user.
            resetTokenRepository.invalidateAllForUser(user.getId());

            // Generate a 32-byte URL-safe token.
            byte[] raw = new byte[32];
            SECURE_RANDOM.nextBytes(raw);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

            String hash = sha256Hex(token);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESET_TTL_MINUTES);
            resetTokenRepository.insert(user.getId(), hash, expiresAt);

            // No email infra in this project — log the token instead.
            log.info("");
            log.info("================ PASSWORD RESET REQUEST ================");
            log.info("User:  {} ({})", user.getUsername(), user.getEmail());
            log.info("Token: {}", token);
            log.info("Valid: {} minutes", RESET_TTL_MINUTES);
            log.info("Use:   POST /api/auth/reset-password");
            log.info("       { \"token\": \"{}\", \"newPassword\": \"...\" }", token);
            log.info("========================================================");
            log.info("");
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Reset token is required");
        }
        String hash = sha256Hex(token.trim());

        PasswordResetTokenRepository.Row row = resetTokenRepository.findActiveByHash(hash)
                .orElseThrow(() -> new AuthenticationException(
                        "Reset token is invalid, expired, or already used"));

        User user = userRepository.findById(row.userId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        userRepository.updatePasswordHash(user.getId(),
                passwordEncoder.encode(newPassword));
        resetTokenRepository.markUsed(row.id());

        log.info("Password reset completed for user id={} username={}",
                user.getId(), user.getUsername());
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
