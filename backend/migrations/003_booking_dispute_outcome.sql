-- Run: psql $DATABASE_URL -f migrations/003_booking_dispute_outcome.sql

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS dispute_resolution_outcome VARCHAR(30);

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS dispute_resolved_at TIMESTAMP;
