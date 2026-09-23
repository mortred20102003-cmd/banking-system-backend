-- =========================================================
-- V4 — transactions
-- Immutable audit log of every money movement.
-- One row per side of a transfer (OUT on source, IN on dest)
-- so per-account history is a simple SELECT by account_id.
-- =========================================================
CREATE TABLE transactions (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    reference_number    VARCHAR(40)    NOT NULL,
    account_id          BIGINT         NOT NULL,
    related_account_id  BIGINT         NULL,
    transaction_type    VARCHAR(20)    NOT NULL,
    amount              DECIMAL(19,4)  NOT NULL,
    balance_before      DECIMAL(19,4)  NOT NULL,
    balance_after       DECIMAL(19,4)  NOT NULL,
    description         VARCHAR(255)   NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT uq_transactions_reference UNIQUE (reference_number),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transactions_related_account
        FOREIGN KEY (related_account_id) REFERENCES accounts(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT')),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;