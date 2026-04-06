const pool = require('./db');
const { sendPush } = require('./fcm');

/**
 * Insert a notification into the DB and fire a FCM push.
 * Used by any route that needs to notify a user.
 */
async function notify(userId, title, message) {
    try {
        await pool.query(
            'INSERT INTO notifications (user_id, title, message) VALUES ($1, $2, $3)',
            [userId, title, message]
        );
        await sendPush(userId, title, message);
    } catch (err) {
        console.error('notify error:', err.message);
    }
}

module.exports = { notify };
