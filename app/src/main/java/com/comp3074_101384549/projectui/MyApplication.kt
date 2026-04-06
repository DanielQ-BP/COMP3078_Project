package com.comp3074_101384549.projectui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.stripe.android.PaymentConfiguration

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ParkSpotFirebaseService.CHANNEL_ID,
                "ParkSpot Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Booking and parking alerts" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}