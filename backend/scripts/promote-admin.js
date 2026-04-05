/**
 * Promote an existing user to admin and print a one-time Admin ID.
 *
 * Usage: node scripts/promote-admin.js <username>
 * Requires DATABASE_URL in .env (same as the API).
 */
require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const pool = require('../src/db');

async function main() {
    const username = process.argv[2];
    if (!username) {
        console.error('Usage: node scripts/promote-admin.js <username>');
        process.exit(1);
    }

    const plainAdminId = `psadm_${crypto.randomBytes(12).toString('base64url')}`;
    const adminIdHash = await bcrypt.hash(plainAdminId, 10);

    const result = await pool.query(
        `UPDATE users
         SET role = 'admin',
             admin_id_hash = $1,
             updated_at = CURRENT_TIMESTAMP
         WHERE username = $2
         RETURNING id, username`,
        [adminIdHash, username]
    );

    if (result.rows.length === 0) {
        console.error(`No user found with username: ${username}`);
        process.exit(1);
    }

    console.log('');
    console.log(`User "${result.rows[0].username}" is now an admin.`);
    console.log('Save this Admin ID in a safe place — it is not stored in plaintext and cannot be shown again:');
    console.log('');
    console.log(`  ${plainAdminId}`);
    console.log('');
    await pool.end();
    process.exit(0);
}

main().catch(async (err) => {
    console.error(err);
    try {
        await pool.end();
    } catch (_) {}
    process.exit(1);
});
