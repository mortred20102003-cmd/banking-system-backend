-- =========================================================
-- V6 — seed a placeholder SuperAdmin row
-- The real BCrypt hash is written by SuperAdminSeeder at
-- application startup (reads SUPER_ADMIN_PASSWORD env var).
-- This row exists so migrations are deterministic and the
-- role constraint is exercised.
-- =========================================================
INSERT INTO users (username, email, password_hash, role, status)
VALUES (
    'MikeyD',
    'mikeyD@bank.com',
    'PENDING_SEED_REPLACE_AT_STARTUP',
    'SUPER_ADMIN',
    'ACTIVE'
);