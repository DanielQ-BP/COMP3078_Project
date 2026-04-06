const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { notify } = require('../notify');

const router = express.Router();

function generateReferenceCode() {
    return `PS-${crypto.randomBytes(4).toString('hex').toUpperCase()}`;
}

// GET /bookings/user/:userId - Get user's bookings
router.get('/user/:userId', authenticateToken, async (req, res) => {
    try {
        const { userId } = req.params;

        // Grace period check: mark confirmed bookings past end_time + 15 min as overstay
        const overstayed = await pool.query(`
            UPDATE bookings
            SET status = 'overstay', updated_at = CURRENT_TIMESTAMP
            WHERE user_id = $1
              AND status = 'confirmed'
              AND end_time + INTERVAL '15 minutes' < NOW()
            RETURNING id, listing_id
        `, [userId]);

        // Create an overstay notification for each newly detected overstay
        for (const booking of overstayed.rows) {
            const listingRes = await pool.query(
                'SELECT address FROM listings WHERE id = $1', [booking.listing_id]
            );
            const address = listingRes.rows[0]?.address || 'your parking spot';
            await notify(
                userId,
                'Overstay Alert',
                `You have exceeded the 15-minute grace period at ${address}. Please vacate immediately to avoid additional charges.`
            );
        }

        const result = await pool.query(`
            SELECT b.id, b.listing_id as "listingId", b.user_id as "userId",
                   b.start_time as "startTime", b.end_time as "endTime",
                   b.total_price as "totalPrice", b.status,
                   b.reference_code as "referenceCode",
                   l.address, l.price_per_hour as "pricePerHour"
            FROM bookings b
            JOIN listings l ON b.listing_id = l.id
            WHERE b.user_id = $1
            ORDER BY b.start_time DESC
        `, [userId]);

        res.json(result.rows);
    } catch (error) {
        console.error('Get bookings error:', error);
        res.status(500).json({ error: 'Failed to fetch bookings' });
    }
});

// GET /bookings/:id - Get single booking
router.get('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(`
            SELECT b.id, b.listing_id as "listingId", b.user_id as "userId",
                   b.start_time as "startTime", b.end_time as "endTime",
                   b.total_price as "totalPrice", b.status,
                   b.reference_code as "referenceCode",
                   l.address, l.price_per_hour as "pricePerHour",
                   l.latitude, l.longitude
            FROM bookings b
            JOIN listings l ON b.listing_id = l.id
            WHERE b.id = $1
        `, [id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Booking not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Get booking error:', error);
        res.status(500).json({ error: 'Failed to fetch booking' });
    }
});

// POST /bookings/create - Create new booking
router.post('/create', authenticateToken, async (req, res) => {
    try {
        const { listingId, startTime, endTime, totalPrice } = req.body;
        const userId = req.user.id;

        if (!listingId || !startTime || !endTime || totalPrice == null) {
            return res.status(400).json({ error: 'listingId, startTime, endTime, and totalPrice are required' });
        }

        // Check for conflicting bookings on the same listing
        const conflict = await pool.query(`
            SELECT COUNT(*) FROM bookings
            WHERE listing_id = $1::uuid
              AND status IN ('confirmed', 'pending', 'overstay')
              AND NOT (end_time <= $2::timestamptz OR start_time >= $3::timestamptz)
        `, [listingId, startTime, endTime]);

        if (parseInt(conflict.rows[0].count) > 0) {
            return res.status(409).json({ error: 'This spot is already booked for the selected time.' });
        }

        let lastError;
        for (let attempt = 0; attempt < 8; attempt++) {
            const referenceCode = generateReferenceCode();
            try {
                const result = await pool.query(`
                    INSERT INTO bookings (listing_id, user_id, start_time, end_time, total_price, status, reference_code)
                    VALUES ($1::uuid, $2::uuid, $3::timestamptz, $4::timestamptz, $5, 'confirmed', $6)
                    RETURNING id, listing_id as "listingId", user_id as "userId",
                              start_time as "startTime", end_time as "endTime",
                              total_price as "totalPrice", status, reference_code as "referenceCode"
                `, [listingId, userId, startTime, endTime, totalPrice, referenceCode]);

                // Notify owner and booker
                const listingRes = await pool.query(
                    'SELECT user_id, address FROM listings WHERE id = $1', [listingId]
                );
                if (listingRes.rows.length > 0) {
                    const { user_id: ownerId, address } = listingRes.rows[0];
                    await notify(ownerId, 'New Booking!',
                        `Your spot at ${address} has been booked.`);
                    await notify(userId, 'Booking Confirmed',
                        `Your booking at ${address} is confirmed. Enjoy your spot!`);
                }

                return res.status(201).json(result.rows[0]);
            } catch (error) {
                lastError = error;
                if (error.code === '23505') {
                    continue;
                }
                throw error;
            }
        }
        console.error('Create booking error (reference collision):', lastError);
        res.status(500).json({ error: 'Failed to assign reservation code' });
    } catch (error) {
        console.error('Create booking error | code:', error.code, '| message:', error.message, '| detail:', error.detail);
        if (error.code === '23503') {
            return res.status(400).json({ error: 'Invalid listing or user ID' });
        }
        if (error.code === '22P02') {
            return res.status(400).json({ error: 'Invalid UUID or timestamp format', detail: error.message });
        }
        if (error.code === '22007' || error.code === '22008') {
            return res.status(400).json({ error: 'Invalid date/time value', detail: error.message });
        }
        res.status(500).json({ error: 'Failed to create booking', detail: error.message });
    }
});

// PUT /bookings/:id - Update booking status
router.put('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { status } = req.body;

        const validStatuses = ['pending', 'confirmed', 'cancelled', 'completed', 'overstay'];
        if (!validStatuses.includes(status)) {
            return res.status(400).json({ error: 'Invalid status' });
        }

        const result = await pool.query(`
            UPDATE bookings
            SET status = $1, updated_at = CURRENT_TIMESTAMP
            WHERE id = $2 AND user_id = $3
            RETURNING id, listing_id as "listingId", user_id as "userId",
                      start_time as "startTime", end_time as "endTime",
                      total_price as "totalPrice", status
        `, [status, id, req.user.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Booking not found or unauthorized' });
        }

        // Notify user on cancellation
        if (status === 'cancelled') {
            const bookingInfo = await pool.query(`
                SELECT l.address FROM bookings b
                JOIN listings l ON b.listing_id = l.id
                WHERE b.id = $1
            `, [id]);
            if (bookingInfo.rows.length > 0) {
                await notify(req.user.id, 'Booking Cancelled',
                    `Your booking at ${bookingInfo.rows[0].address} has been cancelled.`);
            }
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Update booking error:', error);
        res.status(500).json({ error: 'Failed to update booking' });
    }
});

// DELETE /bookings/:id - Cancel booking
router.delete('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        // Get address before deleting for notification
        const bookingInfo = await pool.query(`
            SELECT l.address FROM bookings b
            JOIN listings l ON b.listing_id = l.id
            WHERE b.id = $1 AND b.user_id = $2
        `, [id, req.user.id]);

        const result = await pool.query(
            'DELETE FROM bookings WHERE id = $1 AND user_id = $2 RETURNING id',
            [id, req.user.id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Booking not found or unauthorized' });
        }

        if (bookingInfo.rows.length > 0) {
            await notify(req.user.id, 'Booking Cancelled',
                `Your booking at ${bookingInfo.rows[0].address} has been cancelled.`);
        }

        res.json({ message: 'Booking cancelled successfully' });
    } catch (error) {
        console.error('Delete booking error:', error);
        res.status(500).json({ error: 'Failed to cancel booking' });
    }
});

module.exports = router;
