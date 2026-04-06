const express = require('express');
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { notify } = require('../notify');

const router = express.Router();

// GET /notifications/user/:userId - Get user's notifications
router.get('/user/:userId', authenticateToken, async (req, res) => {
    try {
        const { userId } = req.params;

        const result = await pool.query(`
            SELECT id, user_id as "userId", title, message,
                   is_read as "isRead", created_at as "createdAt"
            FROM notifications
            WHERE user_id = $1
            ORDER BY created_at DESC
        `, [userId]);

        res.json(result.rows);
    } catch (error) {
        console.error('Get notifications error:', error);
        res.status(500).json({ error: 'Failed to fetch notifications' });
    }
});

// PUT /notifications/:id/read - Mark notification as read
router.put('/:id/read', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(`
            UPDATE notifications
            SET is_read = true
            WHERE id = $1 AND user_id = $2
            RETURNING id, user_id as "userId", title, message,
                      is_read as "isRead", created_at as "createdAt"
        `, [id, req.user.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Notification not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Update notification error:', error);
        res.status(500).json({ error: 'Failed to update notification' });
    }
});

// PUT /notifications/read-all - Mark all notifications as read
router.put('/read-all', authenticateToken, async (req, res) => {
    try {
        await pool.query(
            'UPDATE notifications SET is_read = true WHERE user_id = $1 AND is_read = false',
            [req.user.id]
        );
        res.json({ message: 'All notifications marked as read' });
    } catch (error) {
        console.error('Mark all read error:', error);
        res.status(500).json({ error: 'Failed to mark notifications as read' });
    }
});

// POST /notifications/create - Create notification (internal use)
router.post('/create', authenticateToken, async (req, res) => {
    try {
        const { userId, title, message } = req.body;
        await notify(userId, title, message);
        const result = await pool.query(`
            SELECT id, user_id as "userId", title, message,
                   is_read as "isRead", created_at as "createdAt"
            FROM notifications
            WHERE user_id = $1
            ORDER BY created_at DESC LIMIT 1
        `, [userId]);
        res.status(201).json(result.rows[0]);
    } catch (error) {
        console.error('Create notification error:', error);
        res.status(500).json({ error: 'Failed to create notification' });
    }
});

// DELETE /notifications/:id - Delete notification
router.delete('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(
            'DELETE FROM notifications WHERE id = $1 AND user_id = $2 RETURNING id',
            [id, req.user.id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Notification not found' });
        }

        res.json({ message: 'Notification deleted successfully' });
    } catch (error) {
        console.error('Delete notification error:', error);
        res.status(500).json({ error: 'Failed to delete notification' });
    }
});

module.exports = router;
