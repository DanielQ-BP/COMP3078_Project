-- Run once on existing databases: psql $DATABASE_URL -f migrations/001_admin_role.sql

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'user';

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check CHECK (role IN ('user', 'admin'));

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS admin_id_hash VARCHAR(255);
