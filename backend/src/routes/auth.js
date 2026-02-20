const express = require('express');
const bcrypt = require('bcryptjs');
const pool = require('../db');
const { generateToken } = require('../middleware/auth');

const router = express.Router();

// POST /auth/register
router.post('/register', async (req, res) => {
    try {
        const { username, email, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        // Check if user exists
        const existingUser = await pool.query(
            'SELECT id FROM users WHERE username = $1 OR email = $2',
            [username, email || null]
        );

        if (existingUser.rows.length > 0) {
            return res.status(409).json({ error: 'User already exists' });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 10);

        // Create user
        const result = await pool.query(
            'INSERT INTO users (username, email, password) VALUES ($1, $2, $3) RETURNING id, username, email',
            [username, email || null, hashedPassword]
        );

        const user = result.rows[0];
        const token = generateToken(user);

        res.status(201).json(token);
    } catch (error) {
        console.error('Register error:', error);
        res.status(500).json({ error: 'Registration failed' });
    }
});

// POST /auth/login
router.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        // Find user
        const result = await pool.query(
            'SELECT id, username, email, password FROM users WHERE username = $1',
            [username]
        );

        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        const user = result.rows[0];

        // Verify password
        const validPassword = await bcrypt.compare(password, user.password);
        if (!validPassword) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        const token = generateToken(user);

        res.json(token);
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ error: 'Login failed' });
    }
});

module.exports = router;
