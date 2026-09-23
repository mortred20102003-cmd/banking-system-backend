package com.bank.bankingsystem.repository;

import com.bank.bankingsystem.model.Customer;
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
public class CustomerRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CustomerRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------
    // INSERT
    // ---------------------------------------------------------
    private static final String SQL_INSERT = """
            INSERT INTO customers (user_id, first_name, middle_name, last_name, phone, address)
            VALUES (:userId, :firstName, :middleName, :lastName, :phone, :address)
            """;

    public Long insert(Customer customer) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", customer.getUserId())
                .addValue("firstName", customer.getFirstName())
                .addValue("middleName", customer.getMiddleName())
                .addValue("lastName", customer.getLastName())
                .addValue("phone", customer.getPhone())
                .addValue("address", customer.getAddress());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(SQL_INSERT, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to obtain generated customer id");
        return key.longValue();
    }

    // ---------------------------------------------------------
    // SELECT
    // ---------------------------------------------------------
    private static final String SQL_FIND_BY_ID = """
            SELECT id, user_id, first_name, middle_name, last_name, phone, address,
                   created_at, updated_at
            FROM customers
            WHERE id = :id
            """;

    public Optional<Customer> findById(Long id) {
        List<Customer> rows = jdbc.query(SQL_FIND_BY_ID,
                new MapSqlParameterSource("id", id), CUSTOMER_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_USER_ID = """
            SELECT id, user_id, first_name, middle_name, last_name, phone, address,
                   created_at, updated_at
            FROM customers
            WHERE user_id = :userId
            """;

    public Optional<Customer> findByUserId(Long userId) {
        List<Customer> rows = jdbc.query(SQL_FIND_BY_USER_ID,
                new MapSqlParameterSource("userId", userId), CUSTOMER_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_ALL = """
            SELECT id, user_id, first_name, middle_name, last_name, phone, address,
                   created_at, updated_at
            FROM customers
            ORDER BY id
            """;

    public List<Customer> findAll() {
        return jdbc.query(SQL_FIND_ALL, CUSTOMER_ROW_MAPPER);
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------
    private static final String SQL_UPDATE = """
            UPDATE customers
            SET first_name  = COALESCE(:firstName,  first_name),
                middle_name = COALESCE(:middleName, middle_name),
                last_name   = COALESCE(:lastName,   last_name),
                phone       = COALESCE(:phone,      phone),
                address     = COALESCE(:address,    address)
            WHERE id = :id
            """;

    public int update(Customer c) {
        return jdbc.update(SQL_UPDATE,
                new MapSqlParameterSource()
                        .addValue("id", c.getId())
                        .addValue("firstName", c.getFirstName())
                        .addValue("middleName", c.getMiddleName())
                        .addValue("lastName", c.getLastName())
                        .addValue("phone", c.getPhone())
                        .addValue("address", c.getAddress()));
    }

    // ---------------------------------------------------------
    // RowMapper
    // ---------------------------------------------------------
    private static final RowMapper<Customer> CUSTOMER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setFirstName(rs.getString("first_name"));
        c.setMiddleName(rs.getString("middle_name"));
        c.setLastName(rs.getString("last_name"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        c.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return c;
    };

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}