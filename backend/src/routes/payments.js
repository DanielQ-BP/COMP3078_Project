const express = require('express');
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');
const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);

const router = express.Router();

// GET /payments/user/:userId - Get user's payments
router.get('/user/:userId', authenticateToken, async (req, res) => {
    try {
        const { userId } = req.params;

        const result = await pool.query(`
            SELECT p.id, p.booking_id as "bookingId", p.user_id as "userId",
                   p.amount, p.payment_method as "paymentMethod", p.status,
                   p.created_at as "createdAt",
                   b.start_time as "bookingStartTime", l.address
            FROM payments p
            JOIN bookings b ON p.booking_id = b.id
            JOIN listings l ON b.listing_id = l.id
            WHERE p.user_id = $1
            ORDER BY p.created_at DESC
        `, [userId]);

        res.json(result.rows);
    } catch (error) {
        console.error('Get payments error:', error);
        res.status(500).json({ error: 'Failed to fetch payments' });
    }
});

// POST /payments/create - Create payment
router.post('/create', authenticateToken, async (req, res) => {
    try {
        const { bookingId, amount, paymentMethod } = req.body;
        const userId = req.user.id;

        // Create payment
        const paymentResult = await pool.query(`
            INSERT INTO payments (booking_id, user_id, amount, payment_method, status)
            VALUES ($1, $2, $3, $4, 'completed')
            RETURNING id, booking_id as "bookingId", user_id as "userId",
                      amount, payment_method as "paymentMethod", status,
                      created_at as "createdAt"
        `, [bookingId, userId, amount, paymentMethod]);

        // Update booking status to confirmed
        await pool.query(`
            UPDATE bookings SET status = 'confirmed', updated_at = CURRENT_TIMESTAMP
            WHERE id = $1
        `, [bookingId]);

        res.status(201).json(paymentResult.rows[0]);
    } catch (error) {
        console.error('Create payment error:', error);
        res.status(500).json({ error: 'Failed to process payment' });
    }
});

// GET /payments/:id - Get single payment
router.get('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(`
            SELECT p.id, p.booking_id as "bookingId", p.user_id as "userId",
                   p.amount, p.payment_method as "paymentMethod", p.status,
                   p.created_at as "createdAt"
            FROM payments p
            WHERE p.id = $1 AND p.user_id = $2
        `, [id, req.user.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Payment not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Get payment error:', error);
        res.status(500).json({ error: 'Failed to fetch payment' });
    }
});

// POST /payments/create-payment-intent - Create Stripe PaymentIntent
router.post('/create-payment-intent',  async (req, res) => {
    const { amount, currency } = req.body;

    if (!amount || !currency) {
        return res.status(400).json({ error: 'Amount and currency are required.' });
    }

    try {
        const paymentIntent = await stripe.paymentIntents.create({
            amount,
            currency,
            automatic_payment_methods: { enabled: true },
        });

        res.json({ clientSecret: paymentIntent.client_secret });

    } catch (err) {
        console.error('Stripe error:', err.message);
        res.status(500).json({ error: err.message });
    }
});


module.exports = router;
