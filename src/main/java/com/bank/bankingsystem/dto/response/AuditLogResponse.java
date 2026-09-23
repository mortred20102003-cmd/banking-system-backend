package com.bank.bankingsystem.dto.response;

import com.bank.bankingsystem.repository.AdminAuditLogRepository;

import java.time.LocalDateTime;

public class AuditLogResponse {
    private Long id;
    private Long actorUserId;
    private String actorUsername;
    private String action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AdminAuditLogRepository.Row r) {
        AuditLogResponse d = new AuditLogResponse();
        d.id = r.id();
        d.actorUserId = r.actorUserId();
        d.actorUsername = r.actorUsername();
        d.action = r.action();
        d.targetType = r.targetType();
        d.targetId = r.targetId();
        d.details = r.details();
        d.createdAt = r.createdAt();
        return d;
    }

    public Long getId() { return id; }
    public Long getActorUserId() { return actorUserId; }
    public String getActorUsername() { return actorUsername; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
