package com.bank.bankingsystem.repository;

import com.bank.bankingsystem.model.User;
import com.bank.bankingsystem.model.enums.Role;
import com.bank.bankingsystem.model.enums.UserStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------
    // INSERT
    // ---------------------------------------------------------
    private static final String SQL_INSERT = """
            INSERT INTO users (username, email, password_hash, role, status)
            VALUES (:username, :email, :passwordHash, :role, :status)
            """;

    public Long insert(User user) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", user.getUsername())
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("role", user.getRole().name())
                .addValue("status", user.getStatus().name());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(SQL_INSERT, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to obtain generated user id");
        return key.longValue();
    }

    // ---------------------------------------------------------
    // SELECT
    // ---------------------------------------------------------
    private static final String SQL_FIND_BY_ID = """
            SELECT id, username, email, password_hash, role, status, created_at, updated_at
            FROM users
            WHERE id = :id
            """;

    public Optional<User> findById(Long id) {
        List<User> rows = jdbc.query(SQL_FIND_BY_ID,
                new MapSqlParameterSource("id", id), USER_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_USERNAME = """
            SELECT id, username, email, password_hash, role, status, created_at, updated_at
            FROM users
            WHERE username = :username
            """;

    public Optional<User> findByUsername(String username) {
        List<User> rows = jdbc.query(SQL_FIND_BY_USERNAME,
                new MapSqlParameterSource("username", username), USER_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_EMAIL = """
            SELECT id, username, email, password_hash, role, status, created_at, updated_at
            FROM users
            WHERE email = :email
            """;

    public Optional<User> findByEmail(String email) {
        List<User> rows = jdbc.query(SQL_FIND_BY_EMAIL,
                new MapSqlParameterSource("email", email), USER_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_USERNAME_OR_EMAIL = """
            SELECT id, username, email, password_hash, role, status, created_at, updated_at
            FROM users
            WHERE username = :value OR email = :value
            """;

    public Optional<User> findByUsernameOrEmail(String value) {
        List<User> rows = jdbc.query(SQL_FIND_BY_USERNAME_OR_EMAIL,
                new MapSqlParameterSource("value", value), USER_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_ALL = """
            SELECT id, username, email, password_hash, role, status, created_at, updated_at
            FROM users
            ORDER BY id
            """;

    public List<User> findAll() {
        return jdbc.query(SQL_FIND_ALL, USER_ROW_MAPPER);
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------
    private static final String SQL_UPDATE_PASSWORD_HASH = """
            UPDATE users
            SET password_hash = :passwordHash
            WHERE id = :id
            """;

    public int updatePasswordHash(Long id, String passwordHash) {
        return jdbc.update(SQL_UPDATE_PASSWORD_HASH,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("passwordHash", passwordHash));
    }

    private static final String SQL_UPDATE_ROLE = """
            UPDATE users
            SET role = :role
            WHERE id = :id
            """;

    public int updateRole(Long id, Role role) {
        return jdbc.update(SQL_UPDATE_ROLE,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("role", role.name()));
    }

    private static final String SQL_UPDATE_STATUS = """
            UPDATE users
            SET status = :status
            WHERE id = :id
            """;

    public int updateStatus(Long id, UserStatus status) {
        return jdbc.update(SQL_UPDATE_STATUS,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("status", status.name()));
    }

    // ---------------------------------------------------------
    // EXISTS
    // ---------------------------------------------------------
    private static final String SQL_EXISTS_USERNAME = "SELECT COUNT(*) FROM users WHERE username = :username";
    private static final String SQL_EXISTS_EMAIL    = "SELECT COUNT(*) FROM users WHERE email = :email";

    public boolean existsByUsername(String username) {
        Integer c = jdbc.queryForObject(SQL_EXISTS_USERNAME,
                new MapSqlParameterSource("username", username), Integer.class);
        return c != null && c > 0;
    }

    public boolean existsByEmail(String email) {
        Integer c = jdbc.queryForObject(SQL_EXISTS_EMAIL,
                new MapSqlParameterSource("email", email), Integer.class);
        return c != null && c > 0;
    }

    // ---------------------------------------------------------
    // RowMapper
    // ---------------------------------------------------------
    private static final RowMapper<User> USER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setStatus(UserStatus.valueOf(rs.getString("status")));
        u.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        u.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return u;
    };

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}