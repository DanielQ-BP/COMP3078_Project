const express = require('express');
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// GET /listings/all - Get all listings
router.get('/all', authenticateToken, async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT id, user_id as "userId", address, price_per_hour as "pricePerHour",
                   availability, description, is_active as "isActive",
                   latitude, longitude, created_at as "createdAt"
            FROM listings
            WHERE is_active = true
            ORDER BY created_at DESC
        `);

        res.json(result.rows);
    } catch (error) {
        console.error('Get listings error:', error);
        res.status(500).json({ error: 'Failed to fetch listings' });
    }
});

// GET /listings/user/:userId - Get listings by user
router.get('/user/:userId', authenticateToken, async (req, res) => {
    try {
        const { userId } = req.params;

        const result = await pool.query(`
            SELECT id, user_id as "userId", address, price_per_hour as "pricePerHour",
                   availability, description, is_active as "isActive",
                   latitude, longitude, created_at as "createdAt"
            FROM listings
            WHERE user_id = $1
            ORDER BY price_per_hour ASC
        `, [userId]);

        res.json(result.rows);
    } catch (error) {
        console.error('Get user listings error:', error);
        res.status(500).json({ error: 'Failed to fetch user listings' });
    }
});

// GET /listings/:id - Get single listing
router.get('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(`
            SELECT id, user_id as "userId", address, price_per_hour as "pricePerHour",
                   availability, description, is_active as "isActive",
                   latitude, longitude, created_at as "createdAt"
            FROM listings
            WHERE id = $1
        `, [id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Listing not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Get listing error:', error);
        res.status(500).json({ error: 'Failed to fetch listing' });
    }
});

// POST /listings/create - Create new listing
router.post('/create', authenticateToken, async (req, res) => {
    try {
        const { id, address, pricePerHour, availability, description, isActive, latitude, longitude, userId } = req.body;

        const result = await pool.query(`
            INSERT INTO listings (id, user_id, address, price_per_hour, availability, description, is_active, latitude, longitude)
            VALUES (COALESCE($1::uuid, gen_random_uuid()), $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING id, user_id as "userId", address, price_per_hour as "pricePerHour",
                      availability, description, is_active as "isActive",
                      latitude, longitude, created_at as "createdAt"
        `, [id || null, userId || req.user.id, address, pricePerHour, availability, description, isActive !== false, latitude, longitude]);

        res.status(201).json(result.rows[0]);
    } catch (error) {
        console.error('Create listing error:', error);
        res.status(500).json({ error: 'Failed to create listing' });
    }
});

// PUT /listings/:id - Update listing
router.put('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { address, pricePerHour, availability, description, isActive, latitude, longitude } = req.body;

        const result = await pool.query(`
            UPDATE listings
            SET address = COALESCE($1, address),
                price_per_hour = COALESCE($2, price_per_hour),
                availability = COALESCE($3, availability),
                description = COALESCE($4, description),
                is_active = COALESCE($5, is_active),
                latitude = COALESCE($6, latitude),
                longitude = COALESCE($7, longitude),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = $8 AND user_id = $9
            RETURNING id, user_id as "userId", address, price_per_hour as "pricePerHour",
                      availability, description, is_active as "isActive",
                      latitude, longitude
        `, [address, pricePerHour, availability, description, isActive, latitude, longitude, id, req.user.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Listing not found or unauthorized' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Update listing error:', error);
        res.status(500).json({ error: 'Failed to update listing' });
    }
});

// DELETE /listings/:id - Delete listing
router.delete('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(
            'DELETE FROM listings WHERE id = $1 AND user_id = $2 RETURNING id',
            [id, req.user.id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Listing not found or unauthorized' });
        }

        res.json({ message: 'Listing deleted successfully' });
    } catch (error) {
        console.error('Delete listing error:', error);
        res.status(500).json({ error: 'Failed to delete listing' });
    }
});

module.exports = router;
