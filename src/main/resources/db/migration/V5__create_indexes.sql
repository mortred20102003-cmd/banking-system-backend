-- =========================================================
-- V5 — indexes
-- Support lookups and history queries.
-- =========================================================

-- users: fast lookup by email (login) already covered by UNIQUE(email)
CREATE INDEX idx_users_role   ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- customers: search by name for admin views
CREATE INDEX idx_customers_last_name ON customers(last_name);
CREATE INDEX idx_customers_phone     ON customers(phone);

-- accounts: lookup by owner, filter by status/type
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status      ON accounts(status);
CREATE INDEX idx_accounts_type        ON accounts(account_type);
-- note: account_number already uniquely indexed

-- transactions: per-account history sorted by time (most common query)
CREATE INDEX idx_transactions_account_created
    ON transactions(account_id, created_at DESC);
CREATE INDEX idx_transactions_type     ON transactions(transaction_type);
CREATE INDEX idx_transactions_related  ON transactions(related_account_id);