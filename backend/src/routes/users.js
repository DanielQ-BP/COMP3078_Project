const express = require('express');
const bcrypt = require('bcryptjs');
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// GET /users/:id - Get user profile
router.get('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(`
            SELECT id, username, email, created_at as "createdAt"
            FROM users
            WHERE id = $1
        `, [id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({ error: 'Failed to fetch user' });
    }
});

// PUT /users/:id - Update user profile
router.put('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { username, email } = req.body;

        // Only allow users to update their own profile
        if (id !== req.user.id) {
            return res.status(403).json({ error: 'Unauthorized' });
        }

        const result = await pool.query(`
            UPDATE users
            SET username = COALESCE($1, username),
                email = COALESCE($2, email),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = $3
            RETURNING id, username, email, created_at as "createdAt"
        `, [username, email, id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Update user error:', error);
        res.status(500).json({ error: 'Failed to update user' });
    }
});

// PUT /users/:id/password - Change password
router.put('/:id/password', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { currentPassword, newPassword } = req.body;

        // Only allow users to change their own password
        if (id !== req.user.id) {
            return res.status(403).json({ error: 'Unauthorized' });
        }

        // Verify current password
        const userResult = await pool.query(
            'SELECT password FROM users WHERE id = $1',
            [id]
        );

        if (userResult.rows.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        const validPassword = await bcrypt.compare(currentPassword, userResult.rows[0].password);
        if (!validPassword) {
            return res.status(401).json({ error: 'Current password is incorrect' });
        }

        // Hash and update new password
        const hashedPassword = await bcrypt.hash(newPassword, 10);
        await pool.query(
            'UPDATE users SET password = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
            [hashedPassword, id]
        );

        res.json({ message: 'Password updated successfully' });
    } catch (error) {
        console.error('Change password error:', error);
        res.status(500).json({ error: 'Failed to change password' });
    }
});

// PUT /users/:id/fcm-token - Save device FCM token
router.put('/:id/fcm-token', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { token } = req.body;
        if (id !== req.user.id) return res.status(403).json({ error: 'Unauthorized' });
        await pool.query('UPDATE users SET fcm_token = $1 WHERE id = $2', [token, id]);
        res.json({ message: 'FCM token saved' });
    } catch (error) {
        console.error('FCM token error:', error);
        res.status(500).json({ error: 'Failed to save token' });
    }
});

// DELETE /users/:id - Delete user account
router.delete('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        // Only allow users to delete their own account
        if (id !== req.user.id) {
            return res.status(403).json({ error: 'Unauthorized' });
        }

        const result = await pool.query(
            'DELETE FROM users WHERE id = $1 RETURNING id',
            [id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json({ message: 'Account deleted successfully' });
    } catch (error) {
        console.error('Delete user error:', error);
        res.status(500).json({ error: 'Failed to delete account' });
    }
});

module.exports = router;
