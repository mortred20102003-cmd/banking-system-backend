-- =========================================================
-- V7 - Allow a shared reference_number across both legs
--      of a transfer (TRANSFER_OUT and TRANSFER_IN).
--      Replaces the UNIQUE constraint with a plain index.
-- =========================================================
ALTER TABLE transactions DROP INDEX uq_transactions_reference;
CREATE INDEX idx_transactions_reference ON transactions(reference_number);
