-- =========================================================
-- V2 — customers
-- 1:1 profile extension of users (role CUSTOMER).
-- Admins/SuperAdmin may also have a customer profile.
-- =========================================================
CREATE TABLE customers (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    first_name   VARCHAR(60)  NOT NULL,
    middle_name  VARCHAR(60)  NULL,
    last_name    VARCHAR(60)  NOT NULL,
    phone        VARCHAR(20)  NULL,
    address      VARCHAR(255) NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_user_id UNIQUE (user_id),
    CONSTRAINT fk_customers_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;