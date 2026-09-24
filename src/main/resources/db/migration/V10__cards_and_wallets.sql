-- =========================================================
-- V10 - virtual cards + GCash-style wallets
--
-- EDUCATIONAL SIMULATOR ONLY.
-- Card numbers generated here are Luhn-valid but are NOT
-- issued by any bank and will be REJECTED by any real
-- payment terminal.
-- =========================================================

CREATE TABLE cards (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    card_number     VARCHAR(19)  NOT NULL,
    last4           CHAR(4)      NOT NULL,
    brand           VARCHAR(20)  NOT NULL DEFAULT 'VISA',
    cardholder_name VARCHAR(120) NOT NULL,
    expiry_month    TINYINT      NOT NULL,
    expiry_year     SMALLINT     NOT NULL,
    cvv_hash        VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_cards PRIMARY KEY (id),
    CONSTRAINT uq_cards_number UNIQUE (card_number),
    CONSTRAINT fk_cards_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_cards_status CHECK (status IN ('ACTIVE','FROZEN','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cards_customer ON cards(customer_id);
CREATE INDEX idx_cards_account  ON cards(account_id);

CREATE TABLE wallets (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    customer_id  BIGINT         NOT NULL,
    wallet_number VARCHAR(11)   NOT NULL,
    balance      DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    status       VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_wallets PRIMARY KEY (id),
    CONSTRAINT uq_wallets_number UNIQUE (wallet_number),
    CONSTRAINT uq_wallets_customer UNIQUE (customer_id),
    CONSTRAINT fk_wallets_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_wallets_status CHECK (status IN ('ACTIVE','FROZEN')),
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_transactions (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    wallet_id       BIGINT        NOT NULL,
    linked_account_id BIGINT      NULL,
    type            VARCHAR(20)   NOT NULL,
    amount          DECIMAL(19,4) NOT NULL,
    balance_before  DECIMAL(19,4) NOT NULL,
    balance_after   DECIMAL(19,4) NOT NULL,
    reference_number VARCHAR(40)  NOT NULL,
    description     VARCHAR(255)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_wallet_tx PRIMARY KEY (id),
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_wallet_tx_account FOREIGN KEY (linked_account_id) REFERENCES accounts(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_wallet_tx_type CHECK (type IN ('CASH_IN','CASH_OUT')),
    CONSTRAINT chk_wallet_tx_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wallet_tx_wallet ON wallet_transactions(wallet_id, created_at DESC);
