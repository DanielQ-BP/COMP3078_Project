const express = require('express');
const pool = require('../db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

const router = express.Router();

router.use(authenticateToken, requireAdmin);

const BOOKING_OUTCOMES = ['cancelled', 'completed', 'unchanged'];

/**
 * Resolve a conflict ticket and record how the reservation was settled (1.B).
 * POST body: { bookingOutcome: 'cancelled' | 'completed' | 'unchanged' }
 * Sets ticket status to resolved and updates the linked booking.
 */
router.post('/conflicts/:ticketId/resolve', async (req, res) => {
    const { ticketId } = req.params;
    const { bookingOutcome } = req.body;

    if (!BOOKING_OUTCOMES.includes(bookingOutcome)) {
        return res.status(400).json({
            error: 'bookingOutcome must be cancelled, completed, or unchanged',
        });
    }

    const client = await pool.connect();
    try {
        const ticketRow = await client.query(
            `SELECT id, category, booking_id, status FROM tickets WHERE id = $1`,
            [ticketId]
        );

        if (ticketRow.rows.length === 0) {
            return res.status(404).json({ error: 'Ticket not found' });
        }

        const ticket = ticketRow.rows[0];
        if (ticket.category !== 'conflict' || !ticket.booking_id) {
            return res.status(400).json({ error: 'Only conflict tickets linked to a reservation can be resolved here' });
        }

        if (!['open', 'in_progress'].includes(ticket.status)) {
            return res.status(400).json({ error: 'Ticket is not an active dispute' });
        }

        await client.query('BEGIN');

        await client.query(
            `UPDATE tickets SET status = 'resolved', updated_at = CURRENT_TIMESTAMP WHERE id = $1`,
            [ticketId]
        );

        if (bookingOutcome === 'unchanged') {
            await client.query(
                `
                UPDATE bookings
                SET dispute_resolution_outcome = $1,
                    dispute_resolved_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = $2
                `,
                [bookingOutcome, ticket.booking_id]
            );
        } else {
            await client.query(
                `
                UPDATE bookings
                SET status = $1,
                    dispute_resolution_outcome = $2,
                    dispute_resolved_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = $3
                `,
                [bookingOutcome, bookingOutcome, ticket.booking_id]
            );
        }

        await client.query('COMMIT');

        res.json({
            message: 'Dispute resolved',
            ticketId,
            bookingId: ticket.booking_id,
            bookingOutcome,
        });
    } catch (error) {
        try {
            await client.query('ROLLBACK');
        } catch (_) {
            /* ignore */
        }
        console.error('Admin resolve conflict error:', error);
        res.status(500).json({ error: 'Failed to resolve dispute' });
    } finally {
        client.release();
    }
});

/**
 * All reservations with driver/owner names and active conflict flag.
 * Active conflict = open or in_progress ticket with category conflict on this booking.
 */
router.get('/bookings', async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT
                b.id,
                b.reference_code AS "referenceCode",
                b.start_time AS "startTime",
                b.end_time AS "endTime",
                b.total_price AS "totalPrice",
                b.status,
                l.address AS "listingAddress",
                d.username AS "driverUsername",
                d.id AS "driverId",
                o.username AS "ownerUsername",
                o.id AS "ownerId",
                EXISTS (
                    SELECT 1 FROM tickets t
                    WHERE t.booking_id = b.id
                      AND t.category = 'conflict'
                      AND t.status IN ('open', 'in_progress')
                ) AS "hasActiveConflict"
            FROM bookings b
            JOIN listings l ON b.listing_id = l.id
            JOIN users d ON b.user_id = d.id
            JOIN users o ON l.user_id = o.id
            ORDER BY b.start_time DESC
        `);
        res.json(result.rows);
    } catch (error) {
        console.error('Admin bookings list error:', error);
        res.status(500).json({ error: 'Failed to load reservations' });
    }
});

/**
 * Search reservations by reservation code (primary), address, driver, owner, or booking UUID.
 */
router.get('/bookings/search', async (req, res) => {
    try {
        const q = (req.query.q || '').trim();
        if (!q) {
            return res.json([]);
        }

        const pattern = `%${q}%`;
        const upperExact = q.toUpperCase();

        const result = await pool.query(
            `
            SELECT
                b.id,
                b.reference_code AS "referenceCode",
                b.start_time AS "startTime",
                b.end_time AS "endTime",
                b.total_price AS "totalPrice",
                b.status,
                l.address AS "listingAddress",
                d.username AS "driverUsername",
                d.id AS "driverId",
                o.username AS "ownerUsername",
                o.id AS "ownerId",
                EXISTS (
                    SELECT 1 FROM tickets t
                    WHERE t.booking_id = b.id
                      AND t.category = 'conflict'
                      AND t.status IN ('open', 'in_progress')
                ) AS "hasActiveConflict"
            FROM bookings b
            JOIN listings l ON b.listing_id = l.id
            JOIN users d ON b.user_id = d.id
            JOIN users o ON l.user_id = o.id
            WHERE UPPER(TRIM(b.reference_code)) = $1
               OR b.reference_code ILIKE $2
               OR l.address ILIKE $2
               OR d.username ILIKE $2
               OR o.username ILIKE $2
               OR b.id::text ILIKE $2
            ORDER BY b.start_time DESC
            LIMIT 100
            `,
            [upperExact, pattern]
        );

        res.json(result.rows);
    } catch (error) {
        console.error('Admin booking search error:', error);
        res.status(500).json({ error: 'Search failed' });
    }
});

async function conflictHistoryForUser(userId) {
    const history = await pool.query(
        `
        SELECT
            t.id,
            t.subject,
            t.description,
            t.status,
            t.created_at AS "createdAt",
            b.reference_code AS "bookingReferenceCode"
        FROM tickets t
        JOIN bookings b ON t.booking_id = b.id
        JOIN listings l ON b.listing_id = l.id
        WHERE t.category = 'conflict'
          AND (b.user_id = $1 OR l.user_id = $1)
        ORDER BY t.created_at DESC
        `,
        [userId]
    );
    return history.rows;
}

router.get('/bookings/:bookingId/detail', async (req, res) => {
    try {
        const { bookingId } = req.params;

        const bookingResult = await pool.query(
            `
            SELECT
                b.id,
                b.reference_code AS "referenceCode",
                b.start_time AS "startTime",
                b.end_time AS "endTime",
                b.total_price AS "totalPrice",
                b.status,
                b.dispute_resolution_outcome AS "disputeResolutionOutcome",
                b.dispute_resolved_at AS "disputeResolvedAt",
                l.address AS "listingAddress",
                l.id AS "listingId",
                d.id AS "driverId",
                d.username AS "driverUsername",
                d.email AS "driverEmail",
                o.id AS "ownerId",
                o.username AS "ownerUsername",
                o.email AS "ownerEmail"
            FROM bookings b
            JOIN listings l ON b.listing_id = l.id
            JOIN users d ON b.user_id = d.id
            JOIN users o ON l.user_id = o.id
            WHERE b.id = $1
            `,
            [bookingId]
        );

        if (bookingResult.rows.length === 0) {
            return res.status(404).json({ error: 'Booking not found' });
        }

        const row = bookingResult.rows[0];
        const driverId = row.driverId;
        const ownerId = row.ownerId;

        const [driverConflicts, ownerConflicts, activeTickets] = await Promise.all([
            conflictHistoryForUser(driverId),
            conflictHistoryForUser(ownerId),
            pool.query(
                `
                SELECT id, subject, description, status, created_at AS "createdAt"
                FROM tickets
                WHERE booking_id = $1 AND category = 'conflict'
                  AND status IN ('open', 'in_progress')
                ORDER BY created_at ASC
                `,
                [bookingId]
            ),
        ]);

        res.json({
            booking: {
                id: row.id,
                referenceCode: row.referenceCode,
                startTime: row.startTime,
                endTime: row.endTime,
                totalPrice: row.totalPrice,
                status: row.status,
                listingAddress: row.listingAddress,
                listingId: row.listingId,
                disputeResolutionOutcome: row.disputeResolutionOutcome,
                disputeResolvedAt: row.disputeResolvedAt,
            },
            driver: {
                id: driverId,
                username: row.driverUsername,
                email: row.driverEmail,
                previousConflictCount: driverConflicts.length,
                conflicts: driverConflicts.map((c) => ({
                    id: c.id,
                    subject: c.subject,
                    cause: c.description,
                    bookingReferenceCode: c.bookingReferenceCode,
                    status: c.status,
                    createdAt: c.createdAt,
                })),
            },
            owner: {
                id: ownerId,
                username: row.ownerUsername,
                email: row.ownerEmail,
                previousConflictCount: ownerConflicts.length,
                conflicts: ownerConflicts.map((c) => ({
                    id: c.id,
                    subject: c.subject,
                    cause: c.description,
                    bookingReferenceCode: c.bookingReferenceCode,
                    status: c.status,
                    createdAt: c.createdAt,
                })),
            },
            activeConflictTickets: activeTickets.rows,
        });
    } catch (error) {
        console.error('Admin booking detail error:', error);
        res.status(500).json({ error: 'Failed to load booking detail' });
    }
});

router.get('/users', async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT
                u.id,
                u.username,
                u.email,
                u.role,
                (
                    SELECT COUNT(*)::int
                    FROM tickets t
                    JOIN bookings b ON t.booking_id = b.id
                    JOIN listings l ON b.listing_id = l.id
                    WHERE t.category = 'conflict'
                      AND (b.user_id = u.id OR l.user_id = u.id)
                ) AS "conflictCount"
            FROM users u
            ORDER BY u.username ASC
        `);
        res.json(result.rows);
    } catch (error) {
        console.error('Admin users list error:', error);
        res.status(500).json({ error: 'Failed to load users' });
    }
});

router.get('/users/:userId/detail', async (req, res) => {
    try {
        const { userId } = req.params;

        const userResult = await pool.query(
            `SELECT id, username, email, role, created_at AS "createdAt" FROM users WHERE id = $1`,
            [userId]
        );

        if (userResult.rows.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        const user = userResult.rows[0];
        const conflicts = await conflictHistoryForUser(userId);

        res.json({
            user,
            previousConflictCount: conflicts.length,
            conflicts: conflicts.map((c) => ({
                id: c.id,
                subject: c.subject,
                cause: c.description,
                bookingReferenceCode: c.bookingReferenceCode,
                status: c.status,
                createdAt: c.createdAt,
            })),
        });
    } catch (error) {
        console.error('Admin user detail error:', error);
        res.status(500).json({ error: 'Failed to load user detail' });
    }
});

module.exports = router;
