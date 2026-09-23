package com.bank.bankingsystem.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PasswordResetTokenRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PasswordResetTokenRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SQL_INSERT = """
            INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, used)
            VALUES (:userId, :tokenHash, :expiresAt, FALSE)
            """;

    public int insert(Long userId, String tokenHash, LocalDateTime expiresAt) {
        return jdbc.update(SQL_INSERT, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("tokenHash", tokenHash)
                .addValue("expiresAt", Timestamp.valueOf(expiresAt)));
    }

    private static final String SQL_FIND_ACTIVE_BY_HASH = """
            SELECT id, user_id, token_hash, expires_at, used, created_at
            FROM password_reset_tokens
            WHERE token_hash = :tokenHash
              AND used = FALSE
              AND expires_at > NOW()
            ORDER BY id DESC
            LIMIT 1
            """;

    public Optional<Row> findActiveByHash(String tokenHash) {
        List<Row> rows = jdbc.query(SQL_FIND_ACTIVE_BY_HASH,
                new MapSqlParameterSource("tokenHash", tokenHash),
                (rs, i) -> new Row(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("token_hash"),
                        rs.getTimestamp("expires_at").toLocalDateTime(),
                        rs.getBoolean("used"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
        return rows.stream().findFirst();
    }

    private static final String SQL_MARK_USED =
            "UPDATE password_reset_tokens SET used = TRUE WHERE id = :id";

    public int markUsed(Long id) {
        return jdbc.update(SQL_MARK_USED, new MapSqlParameterSource("id", id));
    }

    private static final String SQL_INVALIDATE_FOR_USER =
            "UPDATE password_reset_tokens SET used = TRUE WHERE user_id = :userId AND used = FALSE";

    public int invalidateAllForUser(Long userId) {
        return jdbc.update(SQL_INVALIDATE_FOR_USER, new MapSqlParameterSource("userId", userId));
    }

    public record Row(Long id, Long userId, String tokenHash,
                      LocalDateTime expiresAt, boolean used, LocalDateTime createdAt) { }
}
