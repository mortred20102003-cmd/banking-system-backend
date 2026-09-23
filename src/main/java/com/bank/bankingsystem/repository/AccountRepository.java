package com.bank.bankingsystem.repository;

import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.enums.AccountStatus;
import com.bank.bankingsystem.model.enums.AccountType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AccountRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------
    // INSERT
    // ---------------------------------------------------------
    private static final String SQL_INSERT = """
            INSERT INTO accounts (customer_id, account_number, account_type, balance, status)
            VALUES (:customerId, :accountNumber, :accountType, :balance, :status)
            """;

    public Long insert(Account a) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("customerId", a.getCustomerId())
                .addValue("accountNumber", a.getAccountNumber())
                .addValue("accountType", a.getAccountType().name())
                .addValue("balance", a.getBalance())
                .addValue("status", a.getStatus().name());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(SQL_INSERT, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to obtain generated account id");
        return key.longValue();
    }

    // ---------------------------------------------------------
    // SELECT
    // ---------------------------------------------------------
    private static final String SQL_FIND_BY_ID = """
            SELECT id, customer_id, account_number, account_type, balance, status,
                   created_at, updated_at
            FROM accounts
            WHERE id = :id
            """;

    public Optional<Account> findById(Long id) {
        List<Account> rows = jdbc.query(SQL_FIND_BY_ID,
                new MapSqlParameterSource("id", id), ACCOUNT_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    // Row-locked read — used ONLY inside @Transactional money-movement operations.
    private static final String SQL_FIND_BY_ID_FOR_UPDATE = """
            SELECT id, customer_id, account_number, account_type, balance, status,
                   created_at, updated_at
            FROM accounts
            WHERE id = :id
            FOR UPDATE
            """;

    public Optional<Account> findByIdForUpdate(Long id) {
        List<Account> rows = jdbc.query(SQL_FIND_BY_ID_FOR_UPDATE,
                new MapSqlParameterSource("id", id), ACCOUNT_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_NUMBER = """
            SELECT id, customer_id, account_number, account_type, balance, status,
                   created_at, updated_at
            FROM accounts
            WHERE account_number = :accountNumber
            """;

    public Optional<Account> findByAccountNumber(String accountNumber) {
        List<Account> rows = jdbc.query(SQL_FIND_BY_NUMBER,
                new MapSqlParameterSource("accountNumber", accountNumber), ACCOUNT_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_NUMBER_FOR_UPDATE = """
            SELECT id, customer_id, account_number, account_type, balance, status,
                   created_at, updated_at
            FROM accounts
            WHERE account_number = :accountNumber
            FOR UPDATE
            """;

    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        List<Account> rows = jdbc.query(SQL_FIND_BY_NUMBER_FOR_UPDATE,
                new MapSqlParameterSource("accountNumber", accountNumber), ACCOUNT_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_CUSTOMER = """
            SELECT id, customer_id, account_number, account_type, balance, status,
                   created_at, updated_at
            FROM accounts
            WHERE customer_id = :customerId
            ORDER BY id
            """;

    public List<Account> findByCustomerId(Long customerId) {
        return jdbc.query(SQL_FIND_BY_CUSTOMER,
                new MapSqlParameterSource("customerId", customerId), ACCOUNT_ROW_MAPPER);
    }

    private static final String SQL_FIND_ALL = """
            SELECT id, customer_id, account_number, account_type, balance, status,
                   created_at, updated_at
            FROM accounts
            ORDER BY id
            """;

    public List<Account> findAll() {
        return jdbc.query(SQL_FIND_ALL, ACCOUNT_ROW_MAPPER);
    }

    // ---------------------------------------------------------
    // UPDATE — atomic, concurrency-safe balance operations
    // ---------------------------------------------------------

    // Credit (deposit / transfer-in). Always safe.
    private static final String SQL_CREDIT = """
            UPDATE accounts
            SET balance = balance + :amount
            WHERE id = :id
              AND status = 'ACTIVE'
            """;

    public int credit(Long accountId, BigDecimal amount) {
        return jdbc.update(SQL_CREDIT,
                new MapSqlParameterSource()
                        .addValue("id", accountId)
                        .addValue("amount", amount));
    }

    // Debit (withdrawal / transfer-out). Conditional — the DB refuses to
    // make the balance negative even under concurrent requests.
    private static final String SQL_DEBIT = """
            UPDATE accounts
            SET balance = balance - :amount
            WHERE id = :id
              AND status = 'ACTIVE'
              AND balance >= :amount
            """;

    public int debit(Long accountId, BigDecimal amount) {
        return jdbc.update(SQL_DEBIT,
                new MapSqlParameterSource()
                        .addValue("id", accountId)
                        .addValue("amount", amount));
    }

    private static final String SQL_UPDATE_STATUS = """
            UPDATE accounts
            SET status = :status
            WHERE id = :id
            """;

    public int updateStatus(Long accountId, AccountStatus status) {
        return jdbc.update(SQL_UPDATE_STATUS,
                new MapSqlParameterSource()
                        .addValue("id", accountId)
                        .addValue("status", status.name()));
    }

    // ---------------------------------------------------------
    // EXISTS
    // ---------------------------------------------------------
    private static final String SQL_EXISTS_NUMBER =
            "SELECT COUNT(*) FROM accounts WHERE account_number = :accountNumber";

    public boolean existsByAccountNumber(String accountNumber) {
        Integer c = jdbc.queryForObject(SQL_EXISTS_NUMBER,
                new MapSqlParameterSource("accountNumber", accountNumber), Integer.class);
        return c != null && c > 0;
    }

    private static final String SQL_COUNT_BY_CUSTOMER =
            "SELECT COUNT(*) FROM accounts WHERE customer_id = :customerId";

    public int countByCustomerId(Long customerId) {
        Integer c = jdbc.queryForObject(SQL_COUNT_BY_CUSTOMER,
                new MapSqlParameterSource("customerId", customerId), Integer.class);
        return c == null ? 0 : c;
    }

    // ---------------------------------------------------------
    // Owner-aware queries (join customers for full name)
    // ---------------------------------------------------------

    public record OwnerRow(Account account, String customerName) { }

    private static final String SQL_FIND_ALL_WITH_OWNER = """
            SELECT a.id, a.customer_id, a.account_number, a.account_type, a.balance, a.status,
                   a.created_at, a.updated_at,
                   CONCAT(c.first_name, ' ',
                          COALESCE(CONCAT(c.middle_name, ' '), ''),
                          c.last_name) AS customer_name
            FROM accounts a
            JOIN customers c ON c.id = a.customer_id
            ORDER BY a.id
            """;

    public List<OwnerRow> findAllWithOwner() {
        return jdbc.query(SQL_FIND_ALL_WITH_OWNER, (rs, i) -> new OwnerRow(
                mapAccount(rs),
                rs.getString("customer_name")));
    }

    private static final String SQL_FIND_BY_CUSTOMER_WITH_OWNER = """
            SELECT a.id, a.customer_id, a.account_number, a.account_type, a.balance, a.status,
                   a.created_at, a.updated_at,
                   CONCAT(c.first_name, ' ',
                          COALESCE(CONCAT(c.middle_name, ' '), ''),
                          c.last_name) AS customer_name
            FROM accounts a
            JOIN customers c ON c.id = a.customer_id
            WHERE a.customer_id = :customerId
            ORDER BY a.id
            """;

    public List<OwnerRow> findByCustomerIdWithOwner(Long customerId) {
        return jdbc.query(SQL_FIND_BY_CUSTOMER_WITH_OWNER,
                new MapSqlParameterSource("customerId", customerId),
                (rs, i) -> new OwnerRow(mapAccount(rs), rs.getString("customer_name")));
    }

    private static Account mapAccount(java.sql.ResultSet rs) throws java.sql.SQLException {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setCustomerId(rs.getLong("customer_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountType(com.bank.bankingsystem.model.enums.AccountType.valueOf(rs.getString("account_type")));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(com.bank.bankingsystem.model.enums.AccountStatus.valueOf(rs.getString("status")));
        java.sql.Timestamp ca = rs.getTimestamp("created_at");
        java.sql.Timestamp ua = rs.getTimestamp("updated_at");
        a.setCreatedAt(ca == null ? null : ca.toLocalDateTime());
        a.setUpdatedAt(ua == null ? null : ua.toLocalDateTime());
        return a;
    }

    // ---------------------------------------------------------
    // RowMapper
    // ---------------------------------------------------------
    private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setCustomerId(rs.getLong("customer_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountType(AccountType.valueOf(rs.getString("account_type")));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(AccountStatus.valueOf(rs.getString("status")));
        a.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        a.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return a;
    };

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}