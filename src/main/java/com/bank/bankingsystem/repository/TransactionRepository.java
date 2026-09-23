package com.bank.bankingsystem.repository;

import com.bank.bankingsystem.model.Transaction;
import com.bank.bankingsystem.model.enums.TransactionType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TransactionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------
    // INSERT — append-only ledger, never updated or deleted
    // ---------------------------------------------------------
    private static final String SQL_INSERT = """
            INSERT INTO transactions
                (reference_number, account_id, related_account_id, transaction_type,
                 amount, balance_before, balance_after, description)
            VALUES
                (:referenceNumber, :accountId, :relatedAccountId, :transactionType,
                 :amount, :balanceBefore, :balanceAfter, :description)
            """;

    public Long insert(Transaction t) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("referenceNumber", t.getReferenceNumber())
                .addValue("accountId", t.getAccountId())
                .addValue("relatedAccountId", t.getRelatedAccountId())
                .addValue("transactionType", t.getTransactionType().name())
                .addValue("amount", t.getAmount())
                .addValue("balanceBefore", t.getBalanceBefore())
                .addValue("balanceAfter", t.getBalanceAfter())
                .addValue("description", t.getDescription());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(SQL_INSERT, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to obtain generated transaction id");
        return key.longValue();
    }

    // ---------------------------------------------------------
    // SELECT
    // ---------------------------------------------------------
    private static final String SQL_FIND_BY_ID = """
            SELECT id, reference_number, account_id, related_account_id, transaction_type,
                   amount, balance_before, balance_after, description, created_at
            FROM transactions
            WHERE id = :id
            """;

    public Optional<Transaction> findById(Long id) {
        List<Transaction> rows = jdbc.query(SQL_FIND_BY_ID,
                new MapSqlParameterSource("id", id), TRANSACTION_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    // ---------------------------------------------------------
    // SELECT by reference — returns BOTH legs of a transfer
    // ---------------------------------------------------------
    private static final String SQL_FIND_BY_REFERENCE = """
            SELECT id, reference_number, account_id, related_account_id, transaction_type,
                   amount, balance_before, balance_after, description, created_at
            FROM transactions
            WHERE reference_number = :referenceNumber
            ORDER BY id
            """;

    public List<Transaction> findByReferenceNumber(String referenceNumber) {
        return jdbc.query(SQL_FIND_BY_REFERENCE,
                new MapSqlParameterSource("referenceNumber", referenceNumber),
                TRANSACTION_ROW_MAPPER);
    }

    private static final String SQL_FIND_BY_ACCOUNT = """
            SELECT id, reference_number, account_id, related_account_id, transaction_type,
                   amount, balance_before, balance_after, description, created_at
            FROM transactions
            WHERE account_id = :accountId
            ORDER BY created_at DESC, id DESC
            LIMIT :limit OFFSET :offset
            """;

    public List<Transaction> findByAccountId(Long accountId, int limit, int offset) {
        return jdbc.query(SQL_FIND_BY_ACCOUNT,
                new MapSqlParameterSource()
                        .addValue("accountId", accountId)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                TRANSACTION_ROW_MAPPER);
    }

    private static final String SQL_FIND_BY_ACCOUNT_COUNT =
            "SELECT COUNT(*) FROM transactions WHERE account_id = :accountId";

    public long countByAccountId(Long accountId) {
        Long c = jdbc.queryForObject(SQL_FIND_BY_ACCOUNT_COUNT,
                new MapSqlParameterSource("accountId", accountId), Long.class);
        return c == null ? 0L : c;
    }

    private static final String SQL_FIND_ALL = """
            SELECT id, reference_number, account_id, related_account_id, transaction_type,
                   amount, balance_before, balance_after, description, created_at
            FROM transactions
            ORDER BY created_at DESC, id DESC
            LIMIT :limit OFFSET :offset
            """;

    public List<Transaction> findAll(int limit, int offset) {
        return jdbc.query(SQL_FIND_ALL,
                new MapSqlParameterSource()
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                TRANSACTION_ROW_MAPPER);
    }

    // ---------------------------------------------------------
    // Enriched queries — include account number + owner name,
    // and the related account (for transfers).
    // ---------------------------------------------------------

    public record TransactionRow(Transaction transaction,
                                 String accountNumber,
                                 String accountOwnerName,
                                 String relatedAccountNumber,
                                 String relatedAccountOwnerName) { }

    private static final String SQL_FIND_BY_ACCOUNT_ENRICHED = """
            SELECT t.id, t.reference_number, t.account_id, t.related_account_id,
                   t.transaction_type, t.amount, t.balance_before, t.balance_after,
                   t.description, t.created_at,
                   a.account_number AS account_number,
                   CONCAT(c.first_name, ' ', c.last_name) AS account_owner_name,
                   ra.account_number AS related_account_number,
                   CONCAT(rc.first_name, ' ', rc.last_name) AS related_owner_name
            FROM transactions t
            JOIN accounts  a  ON a.id  = t.account_id
            JOIN customers c  ON c.id  = a.customer_id
            LEFT JOIN accounts  ra ON ra.id = t.related_account_id
            LEFT JOIN customers rc ON rc.id = ra.customer_id
            WHERE t.account_id = :accountId
            ORDER BY t.created_at DESC, t.id DESC
            LIMIT :limit OFFSET :offset
            """;

    public List<TransactionRow> findByAccountIdEnriched(Long accountId, int limit, int offset) {
        return jdbc.query(SQL_FIND_BY_ACCOUNT_ENRICHED,
                new MapSqlParameterSource()
                        .addValue("accountId", accountId)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                (rs, i) -> new TransactionRow(
                        mapTransaction(rs),
                        rs.getString("account_number"),
                        rs.getString("account_owner_name"),
                        rs.getString("related_account_number"),
                        rs.getString("related_owner_name")));
    }

    private static final String SQL_FIND_BY_REFERENCE_ENRICHED = """
            SELECT t.id, t.reference_number, t.account_id, t.related_account_id,
                   t.transaction_type, t.amount, t.balance_before, t.balance_after,
                   t.description, t.created_at,
                   a.account_number AS account_number,
                   CONCAT(c.first_name, ' ', c.last_name) AS account_owner_name,
                   ra.account_number AS related_account_number,
                   CONCAT(rc.first_name, ' ', rc.last_name) AS related_owner_name
            FROM transactions t
            JOIN accounts  a  ON a.id  = t.account_id
            JOIN customers c  ON c.id  = a.customer_id
            LEFT JOIN accounts  ra ON ra.id = t.related_account_id
            LEFT JOIN customers rc ON rc.id = ra.customer_id
            WHERE t.reference_number = :referenceNumber
            ORDER BY t.id
            """;

    public List<TransactionRow> findByReferenceNumberEnriched(String referenceNumber) {
        return jdbc.query(SQL_FIND_BY_REFERENCE_ENRICHED,
                new MapSqlParameterSource("referenceNumber", referenceNumber),
                (rs, i) -> new TransactionRow(
                        mapTransaction(rs),
                        rs.getString("account_number"),
                        rs.getString("account_owner_name"),
                        rs.getString("related_account_number"),
                        rs.getString("related_owner_name")));
    }

    private static Transaction mapTransaction(java.sql.ResultSet rs) throws java.sql.SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setReferenceNumber(rs.getString("reference_number"));
        t.setAccountId(rs.getLong("account_id"));
        long related = rs.getLong("related_account_id");
        t.setRelatedAccountId(rs.wasNull() ? null : related);
        t.setTransactionType(com.bank.bankingsystem.model.enums.TransactionType.valueOf(rs.getString("transaction_type")));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceBefore(rs.getBigDecimal("balance_before"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setDescription(rs.getString("description"));
        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        t.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return t;
    }

    // ---------------------------------------------------------
    // RowMapper
    // ---------------------------------------------------------
    private static final RowMapper<Transaction> TRANSACTION_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setReferenceNumber(rs.getString("reference_number"));
        t.setAccountId(rs.getLong("account_id"));

        long related = rs.getLong("related_account_id");
        t.setRelatedAccountId(rs.wasNull() ? null : related);

        t.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceBefore(rs.getBigDecimal("balance_before"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setDescription(rs.getString("description"));
        t.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return t;
    };

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}