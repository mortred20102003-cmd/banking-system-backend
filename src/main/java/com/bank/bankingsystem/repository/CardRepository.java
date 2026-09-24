package com.bank.bankingsystem.repository;

import com.bank.bankingsystem.model.Card;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class CardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CardRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String SQL_INSERT = """
            INSERT INTO cards (customer_id, account_id, card_number, last4, brand,
                               cardholder_name, expiry_month, expiry_year, cvv_hash, status)
            VALUES (:customerId, :accountId, :cardNumber, :last4, :brand,
                    :cardholderName, :expiryMonth, :expiryYear, :cvvHash, 'ACTIVE')
            """;

    public Long insert(Card c) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(SQL_INSERT, new MapSqlParameterSource()
                .addValue("customerId", c.getCustomerId())
                .addValue("accountId", c.getAccountId())
                .addValue("cardNumber", c.getCardNumber())
                .addValue("last4", c.getLast4())
                .addValue("brand", c.getBrand())
                .addValue("cardholderName", c.getCardholderName())
                .addValue("expiryMonth", c.getExpiryMonth())
                .addValue("expiryYear", c.getExpiryYear())
                .addValue("cvvHash", c.getCvvHash()), kh, new String[]{"id"});
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    private static final String SQL_FIND_ALL_BY_CUSTOMER = """
            SELECT * FROM cards WHERE customer_id = :customerId ORDER BY id
            """;

    public List<Card> findByCustomerId(Long customerId) {
        return jdbc.query(SQL_FIND_ALL_BY_CUSTOMER,
                new MapSqlParameterSource("customerId", customerId), MAPPER);
    }

    private static final String SQL_FIND_BY_ID = "SELECT * FROM cards WHERE id = :id";
    public Optional<Card> findById(Long id) {
        List<Card> rows = jdbc.query(SQL_FIND_BY_ID,
                new MapSqlParameterSource("id", id), MAPPER);
        return rows.stream().findFirst();
    }

    private static final String SQL_EXISTS_NUMBER =
            "SELECT COUNT(*) FROM cards WHERE card_number = :num";
    public boolean existsByNumber(String num) {
        Integer c = jdbc.queryForObject(SQL_EXISTS_NUMBER,
                new MapSqlParameterSource("num", num), Integer.class);
        return c != null && c > 0;
    }

    private static final String SQL_UPDATE_STATUS =
            "UPDATE cards SET status = :status WHERE id = :id";
    public int updateStatus(Long id, String status) {
        return jdbc.update(SQL_UPDATE_STATUS,
                new MapSqlParameterSource().addValue("id", id).addValue("status", status));
    }

    private static final String SQL_COUNT_BY_CUSTOMER =
            "SELECT COUNT(*) FROM cards WHERE customer_id = :customerId";
    public int countByCustomerId(Long customerId) {
        Integer c = jdbc.queryForObject(SQL_COUNT_BY_CUSTOMER,
                new MapSqlParameterSource("customerId", customerId), Integer.class);
        return c == null ? 0 : c;
    }

    private static final RowMapper<Card> MAPPER = (ResultSet rs, int i) -> {
        Card c = new Card();
        c.setId(rs.getLong("id"));
        c.setCustomerId(rs.getLong("customer_id"));
        c.setAccountId(rs.getLong("account_id"));
        c.setCardNumber(rs.getString("card_number"));
        c.setLast4(rs.getString("last4"));
        c.setBrand(rs.getString("brand"));
        c.setCardholderName(rs.getString("cardholder_name"));
        c.setExpiryMonth(rs.getInt("expiry_month"));
        c.setExpiryYear(rs.getInt("expiry_year"));
        c.setCvvHash(rs.getString("cvv_hash"));
        c.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        c.setCreatedAt(ca == null ? null : ca.toLocalDateTime());
        c.setUpdatedAt(ua == null ? null : ua.toLocalDateTime());
        return c;
    };
}
