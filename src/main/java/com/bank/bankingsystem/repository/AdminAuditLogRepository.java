package com.bank.bankingsystem.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AdminAuditLogRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdminAuditLogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SQL_INSERT = """
            INSERT INTO admin_audit_log (actor_user_id, action, target_type, target_id, details)
            VALUES (:actorUserId, :action, :targetType, :targetId, :details)
            """;

    public void log(Long actorUserId, String action, String targetType, Long targetId, String details) {
        jdbc.update(SQL_INSERT, new MapSqlParameterSource()
                .addValue("actorUserId", actorUserId)
                .addValue("action", action)
                .addValue("targetType", targetType)
                .addValue("targetId", targetId)
                .addValue("details", details));
    }

    private static final String SQL_FIND_RECENT = """
            SELECT l.id, l.actor_user_id, u.username AS actor_username,
                   l.action, l.target_type, l.target_id, l.details, l.created_at
            FROM admin_audit_log l
            JOIN users u ON u.id = l.actor_user_id
            ORDER BY l.created_at DESC, l.id DESC
            LIMIT :limit OFFSET :offset
            """;

    public List<Row> findRecent(int limit, int offset) {
        return jdbc.query(SQL_FIND_RECENT,
                new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset),
                (rs, i) -> new Row(
                        rs.getLong("id"),
                        rs.getLong("actor_user_id"),
                        rs.getString("actor_username"),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        rs.getObject("target_id") == null ? null : rs.getLong("target_id"),
                        rs.getString("details"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
    }

    private static final String SQL_COUNT = "SELECT COUNT(*) FROM admin_audit_log";

    public long count() {
        Long c = jdbc.queryForObject(SQL_COUNT, new MapSqlParameterSource(), Long.class);
        return c == null ? 0L : c;
    }

    public record Row(Long id, Long actorUserId, String actorUsername,
                      String action, String targetType, Long targetId,
                      String details, LocalDateTime createdAt) { }
}
