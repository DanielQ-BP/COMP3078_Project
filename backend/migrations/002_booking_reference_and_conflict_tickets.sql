-- Run: psql $DATABASE_URL -f migrations/002_booking_reference_and_conflict_tickets.sql

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS reference_code VARCHAR(32);

CREATE UNIQUE INDEX IF NOT EXISTS idx_bookings_reference_code
    ON bookings (reference_code)
    WHERE reference_code IS NOT NULL;

UPDATE bookings
SET reference_code = 'PS-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
WHERE reference_code IS NULL;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS category VARCHAR(20);

UPDATE tickets SET category = 'general' WHERE category IS NULL;

ALTER TABLE tickets
    ALTER COLUMN category SET DEFAULT 'general';

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS booking_id UUID REFERENCES bookings(id) ON DELETE SET NULL;
