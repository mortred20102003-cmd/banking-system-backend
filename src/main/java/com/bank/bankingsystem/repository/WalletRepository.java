package com.bank.bankingsystem.repository;

import com.bank.bankingsystem.model.Wallet;
import com.bank.bankingsystem.model.WalletTransaction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class WalletRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public WalletRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    // ---- wallet ----

    private static final String SQL_INSERT_WALLET = """
            INSERT INTO wallets (customer_id, wallet_number, balance, status)
            VALUES (:customerId, :walletNumber, 0.0000, 'ACTIVE')
            """;

    public Long insertWallet(Wallet w) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(SQL_INSERT_WALLET, new MapSqlParameterSource()
                .addValue("customerId", w.getCustomerId())
                .addValue("walletNumber", w.getWalletNumber()), kh, new String[]{"id"});
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    private static final String SQL_FIND_BY_CUSTOMER =
            "SELECT * FROM wallets WHERE customer_id = :customerId";
    public Optional<Wallet> findByCustomerId(Long customerId) {
        List<Wallet> rows = jdbc.query(SQL_FIND_BY_CUSTOMER,
                new MapSqlParameterSource("customerId", customerId), WALLET_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_FIND_BY_ID_FOR_UPDATE =
            "SELECT * FROM wallets WHERE id = :id FOR UPDATE";
    public Optional<Wallet> findByIdForUpdate(Long id) {
        List<Wallet> rows = jdbc.query(SQL_FIND_BY_ID_FOR_UPDATE,
                new MapSqlParameterSource("id", id), WALLET_MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_CREDIT =
            "UPDATE wallets SET balance = balance + :amt WHERE id = :id AND status = 'ACTIVE'";
    public int credit(Long id, BigDecimal amount) {
        return jdbc.update(SQL_CREDIT,
                new MapSqlParameterSource().addValue("id", id).addValue("amt", amount));
    }

    private static final String SQL_DEBIT = """
            UPDATE wallets SET balance = balance - :amt
            WHERE id = :id AND status = 'ACTIVE' AND balance >= :amt
            """;
    public int debit(Long id, BigDecimal amount) {
        return jdbc.update(SQL_DEBIT,
                new MapSqlParameterSource().addValue("id", id).addValue("amt", amount));
    }

    private static final String SQL_EXISTS_NUMBER =
            "SELECT COUNT(*) FROM wallets WHERE wallet_number = :num";
    public boolean existsByNumber(String num) {
        Integer c = jdbc.queryForObject(SQL_EXISTS_NUMBER,
                new MapSqlParameterSource("num", num), Integer.class);
        return c != null && c > 0;
    }

    // ---- wallet transactions ----

    private static final String SQL_INSERT_TX = """
            INSERT INTO wallet_transactions
              (wallet_id, linked_account_id, type, amount, balance_before,
               balance_after, reference_number, description)
            VALUES (:walletId, :linkedAccountId, :type, :amount, :before,
                    :after, :reference, :description)
            """;

    public void insertTx(WalletTransaction tx) {
        jdbc.update(SQL_INSERT_TX, new MapSqlParameterSource()
                .addValue("walletId", tx.getWalletId())
                .addValue("linkedAccountId", tx.getLinkedAccountId())
                .addValue("type", tx.getType())
                .addValue("amount", tx.getAmount())
                .addValue("before", tx.getBalanceBefore())
                .addValue("after", tx.getBalanceAfter())
                .addValue("reference", tx.getReferenceNumber())
                .addValue("description", tx.getDescription()));
    }

    private static final String SQL_FIND_TX = """
            SELECT * FROM wallet_transactions
            WHERE wallet_id = :walletId
            ORDER BY created_at DESC, id DESC
            LIMIT :limit OFFSET :offset
            """;

    public List<WalletTransaction> findTransactions(Long walletId, int limit, int offset) {
        return jdbc.query(SQL_FIND_TX,
                new MapSqlParameterSource()
                        .addValue("walletId", walletId)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                WALLET_TX_MAPPER);
    }

    private static final RowMapper<Wallet> WALLET_MAPPER = (ResultSet rs, int i) -> {
        Wallet w = new Wallet();
        w.setId(rs.getLong("id"));
        w.setCustomerId(rs.getLong("customer_id"));
        w.setWalletNumber(rs.getString("wallet_number"));
        w.setBalance(rs.getBigDecimal("balance"));
        w.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        w.setCreatedAt(ca == null ? null : ca.toLocalDateTime());
        w.setUpdatedAt(ua == null ? null : ua.toLocalDateTime());
        return w;
    };

    private static final RowMapper<WalletTransaction> WALLET_TX_MAPPER = (ResultSet rs, int i) -> {
        WalletTransaction t = new WalletTransaction();
        t.setId(rs.getLong("id"));
        t.setWalletId(rs.getLong("wallet_id"));
        long linked = rs.getLong("linked_account_id");
        t.setLinkedAccountId(rs.wasNull() ? null : linked);
        t.setType(rs.getString("type"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceBefore(rs.getBigDecimal("balance_before"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setReferenceNumber(rs.getString("reference_number"));
        t.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        t.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return t;
    };
}
