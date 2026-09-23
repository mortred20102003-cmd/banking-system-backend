-- =========================================================
-- V9 - admin audit log
-- Every privileged action (promote/demote/status/credit/edit)
-- is recorded so we can answer "who did what, when".
-- =========================================================
CREATE TABLE admin_audit_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    actor_user_id  BIGINT       NOT NULL,
    action         VARCHAR(60)  NOT NULL,
    target_type    VARCHAR(40)  NOT NULL,
    target_id      BIGINT       NULL,
    details        VARCHAR(500) NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_admin_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_actor   ON admin_audit_log(actor_user_id);
CREATE INDEX idx_audit_created ON admin_audit_log(created_at DESC);
CREATE INDEX idx_audit_target  ON admin_audit_log(target_type, target_id);
