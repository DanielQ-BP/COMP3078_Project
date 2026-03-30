const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');

dotenv.config();

const authRoutes = require('./src/routes/auth');
const listingRoutes = require('./src/routes/listings');
const bookingRoutes = require('./src/routes/bookings');
const paymentRoutes = require('./src/routes/payments');
const notificationRoutes = require('./src/routes/notifications');
const userRoutes = require('./src/routes/users');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// Routes
app.use('/auth', authRoutes);
app.use('/listings', listingRoutes);
app.use('/bookings', bookingRoutes);
app.use('/payments', paymentRoutes);
app.use('/notifications', notificationRoutes);
app.use('/users', userRoutes);

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Something went wrong!' });
});

app.listen(PORT, () => {
    console.log(`ParkSpot API running on port ${PORT}`);
});
