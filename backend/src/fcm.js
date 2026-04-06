const admin = require('firebase-admin');
const pool = require('./db');

// Initialize Firebase Admin once
(function init() {
    if (admin.apps.length > 0) return;
    const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
    if (!raw) {
        console.warn('[FCM] FIREBASE_SERVICE_ACCOUNT not set — push notifications disabled');
        return;
    }
    try {
        const serviceAccount = JSON.parse(raw);
        admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
        console.log('[FCM] Firebase Admin initialized');
    } catch (err) {
        console.error('[FCM] Init error:', err.message);
    }
})();

/**
 * Send a push notification to a user by userId.
 * Silently no-ops if FCM is not configured or user has no token.
 */
async function sendPush(userId, title, body) {
    if (admin.apps.length === 0) return;
    try {
        const result = await pool.query('SELECT fcm_token FROM users WHERE id = $1', [userId]);
        const token = result.rows[0]?.fcm_token;
        if (!token) return;
        await admin.messaging().send({
            token,
            notification: { title, body },
            android: { priority: 'high' }
        });
    } catch (err) {
        // Token may be stale — not a fatal error
        console.error('[FCM] sendPush error:', err.message);
    }
}

module.exports = { sendPush };
