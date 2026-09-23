-- =========================================================
-- V1 — users
-- Stores authentication identities for all actors:
-- CUSTOMER, ADMIN, SUPER_ADMIN
-- =========================================================
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('CUSTOMER','ADMIN','SUPER_ADMIN')),
    CONSTRAINT chk_users_status  CHECK (status IN ('ACTIVE','INACTIVE','LOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;