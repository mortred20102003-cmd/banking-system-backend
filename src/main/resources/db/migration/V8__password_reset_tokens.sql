-- =========================================================
-- V8 - password reset tokens
-- Backs a "forgot password" flow. We store only the SHA-256
-- hash of the token, not the token itself, so a DB leak does
-- not expose usable reset links.
-- =========================================================
CREATE TABLE password_reset_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  CHAR(64)     NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_prt_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_prt_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_prt_user_id    ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_expires_at ON password_reset_tokens(expires_at);
