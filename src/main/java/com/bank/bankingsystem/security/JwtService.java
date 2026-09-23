package com.bank.bankingsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.issuer:Banking_System}") String issuer) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 bytes (256 bits). Check JWT_SECRET in .env");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generateToken(AuthenticatedUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .claims(Map.of(
                        "uid", user.getId(),
                        "email", user.getEmail(),
                        "role", user.getRole()
                ))
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Parses and validates the token. Returns claims on success.
     * Throws {@link JwtException} on any tampering/expiry issue.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }
}
