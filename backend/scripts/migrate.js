require('dotenv').config();
const pool = require('../src/db');

async function migrate() {
    const client = await pool.connect();
    try {
        console.log('Running migrations...');

        // Migration 001 — admin role
        await client.query(`
            ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'user';
        `);
        await client.query(`
            ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
        `);
        await client.query(`
            ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('user', 'admin'));
        `);
        await client.query(`
            ALTER TABLE users ADD COLUMN IF NOT EXISTS admin_id_hash VARCHAR(255);
        `);
        console.log('✓ Migration 001 — admin role');

        // Migration 002 — booking reference_code + conflict tickets
        await client.query(`
            ALTER TABLE bookings ADD COLUMN IF NOT EXISTS reference_code VARCHAR(32);
        `);
        await client.query(`
            CREATE UNIQUE INDEX IF NOT EXISTS idx_bookings_reference_code
            ON bookings (reference_code)
            WHERE reference_code IS NOT NULL;
        `);
        await client.query(`
            UPDATE bookings
            SET reference_code = 'PS-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
            WHERE reference_code IS NULL;
        `);
        await client.query(`
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS category VARCHAR(20);
        `);
        await client.query(`
            UPDATE tickets SET category = 'general' WHERE category IS NULL;
        `);
        await client.query(`
            ALTER TABLE tickets ALTER COLUMN category SET DEFAULT 'general';
        `);
        await client.query(`
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS booking_id UUID REFERENCES bookings(id) ON DELETE SET NULL;
        `);
        console.log('✓ Migration 002 — reference_code + conflict tickets');

        // Migration 003 — booking dispute outcome
        await client.query(`
            ALTER TABLE bookings ADD COLUMN IF NOT EXISTS dispute_resolution_outcome VARCHAR(30);
        `);
        await client.query(`
            ALTER TABLE bookings ADD COLUMN IF NOT EXISTS dispute_resolved_at TIMESTAMP;
        `);
        console.log('✓ Migration 003 — dispute outcome columns');

        // Migration 004 — FCM push token
        await client.query(`
            ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token TEXT;
        `);
        console.log('✓ Migration 004 — fcm_token column');

        console.log('\nAll migrations applied successfully.');
    } catch (err) {
        console.error('Migration failed:', err.message);
        process.exit(1);
    } finally {
        client.release();
        await pool.end();
    }
}

migrate();
