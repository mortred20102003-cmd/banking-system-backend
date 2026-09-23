-- =========================================================
-- V3 — accounts
-- Bank accounts owned by a customer.
-- Money stored as DECIMAL(19,4). NEVER float/double.
-- =========================================================
CREATE TABLE accounts (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT         NOT NULL,
    account_number  VARCHAR(20)    NOT NULL,
    account_type    VARCHAR(20)    NOT NULL,
    balance         DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_accounts_type    CHECK (account_type IN ('SAVINGS','CHECKING')),
    CONSTRAINT chk_accounts_status  CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;