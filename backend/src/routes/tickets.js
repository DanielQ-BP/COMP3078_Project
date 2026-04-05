const express = require('express');
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// POST /tickets/create
router.post('/create', authenticateToken, async (req, res) => {
    try {
        const { subject, description, category, bookingReferenceCode } = req.body;
        const userId = req.user.id;

        if (!subject || !description) {
            return res.status(400).json({ error: 'Subject and description are required' });
        }

        const cat = category === 'conflict' ? 'conflict' : 'general';
        let bookingId = null;

        if (cat === 'conflict') {
            const code = (bookingReferenceCode || '').trim().toUpperCase();
            if (!code) {
                return res.status(400).json({
                    error: 'Reservation code is required to report a dispute. Find it in My Reservations after you book.',
                });
            }

            const bookingResult = await pool.query(
                `SELECT id, user_id FROM bookings WHERE UPPER(TRIM(reference_code)) = $1`,
                [code]
            );

            if (bookingResult.rows.length === 0) {
                return res.status(400).json({ error: 'No reservation matches that code. Check and try again.' });
            }

            const booking = bookingResult.rows[0];
            if (booking.user_id !== userId) {
                return res.status(403).json({ error: 'You can only open a dispute for your own reservations.' });
            }

            bookingId = booking.id;
        }

        const result = await pool.query(`
            INSERT INTO tickets (user_id, subject, description, category, booking_id)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING id, user_id as "userId", subject, description,
                      status, category, booking_id as "bookingId", created_at as "createdAt"
        `, [userId, subject, description, cat, bookingId]);

        res.status(201).json(result.rows[0]);
    } catch (error) {
        console.error('Create ticket error:', error);
        res.status(500).json({ error: 'Failed to create ticket' });
    }
});

// GET /tickets/my
router.get('/my', authenticateToken, async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT id, user_id as "userId", subject, description,
                   status, category, booking_id as "bookingId",
                   created_at as "createdAt", updated_at as "updatedAt"
            FROM tickets
            WHERE user_id = $1
            ORDER BY created_at DESC
        `, [req.user.id]);

        res.json(result.rows);
    } catch (error) {
        console.error('Get tickets error:', error);
        res.status(500).json({ error: 'Failed to fetch tickets' });
    }
});

// GET /tickets/all - Admin only
router.get('/all', authenticateToken, async (req, res) => {
    try {
        if (req.user.role !== 'admin') {
            return res.status(403).json({ error: 'Admin access required' });
        }

        const result = await pool.query(`
            SELECT t.id, t.user_id as "userId", u.username,
                   t.subject, t.description, t.status,
                   t.created_at as "createdAt", t.updated_at as "updatedAt"
            FROM tickets t
            JOIN users u ON t.user_id = u.id
            ORDER BY t.created_at DESC
        `);

        res.json(result.rows);
    } catch (error) {
        console.error('Get all tickets error:', error);
        res.status(500).json({ error: 'Failed to fetch tickets' });
    }
});

// GET /tickets/:id
router.get('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const isAdmin = req.user.role === 'admin';

        const ticketResult = await pool.query(`
            SELECT t.id, t.user_id as "userId", u.username,
                   t.subject, t.description, t.status,
                   t.category, t.booking_id as "bookingId",
                   t.created_at as "createdAt", t.updated_at as "updatedAt"
            FROM tickets t
            JOIN users u ON t.user_id = u.id
            WHERE t.id = $1
        `, [id]);

        if (ticketResult.rows.length === 0) {
            return res.status(404).json({ error: 'Ticket not found' });
        }

        const ticket = ticketResult.rows[0];
        if (ticket.userId !== req.user.id && !isAdmin) {
            return res.status(403).json({ error: 'Unauthorized' });
        }

        const responsesResult = await pool.query(`
            SELECT r.id, r.ticket_id as "ticketId", r.user_id as "userId",
                   u.username, r.message, r.is_admin_response as "isAdminResponse",
                   r.created_at as "createdAt"
            FROM ticket_responses r
            JOIN users u ON r.user_id = u.id
            WHERE r.ticket_id = $1
            ORDER BY r.created_at ASC
        `, [id]);

        res.json({ ...ticket, responses: responsesResult.rows });
    } catch (error) {
        console.error('Get ticket error:', error);
        res.status(500).json({ error: 'Failed to fetch ticket' });
    }
});

// POST /tickets/:id/respond
router.post('/:id/respond', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { message } = req.body;
        const isAdmin = req.user.role === 'admin';

        if (!message) {
            return res.status(400).json({ error: 'Message is required' });
        }

        const ticketResult = await pool.query(
            'SELECT user_id, status FROM tickets WHERE id = $1', [id]
        );

        if (ticketResult.rows.length === 0) {
            return res.status(404).json({ error: 'Ticket not found' });
        }

        const ticket = ticketResult.rows[0];
        if (ticket.user_id !== req.user.id && !isAdmin) {
            return res.status(403).json({ error: 'Unauthorized' });
        }

        const result = await pool.query(`
            INSERT INTO ticket_responses (ticket_id, user_id, message, is_admin_response)
            VALUES ($1, $2, $3, $4)
            RETURNING id, ticket_id as "ticketId", user_id as "userId",
                      message, is_admin_response as "isAdminResponse",
                      created_at as "createdAt"
        `, [id, req.user.id, message, isAdmin]);

        if (isAdmin && ticket.status === 'open') {
            await pool.query(
                `UPDATE tickets SET status = 'in_progress', updated_at = CURRENT_TIMESTAMP WHERE id = $1`,
                [id]
            );
        }

        res.status(201).json(result.rows[0]);
    } catch (error) {
        console.error('Respond to ticket error:', error);
        res.status(500).json({ error: 'Failed to add response' });
    }
});

// PUT /tickets/:id/status - Admin only
router.put('/:id/status', authenticateToken, async (req, res) => {
    try {
        if (req.user.role !== 'admin') {
            return res.status(403).json({ error: 'Admin access required' });
        }

        const { id } = req.params;
        const { status } = req.body;
        const validStatuses = ['open', 'in_progress', 'resolved', 'closed'];

        if (!validStatuses.includes(status)) {
            return res.status(400).json({ error: 'Invalid status' });
        }

        const result = await pool.query(`
            UPDATE tickets SET status = $1, updated_at = CURRENT_TIMESTAMP
            WHERE id = $2
            RETURNING id, status, updated_at as "updatedAt"
        `, [status, id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Ticket not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Update ticket status error:', error);
        res.status(500).json({ error: 'Failed to update ticket status' });
    }
});

module.exports = router;