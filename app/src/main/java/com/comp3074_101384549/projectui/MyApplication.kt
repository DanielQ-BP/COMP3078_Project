package com.comp3074_101384549.projectui

import android.app.Application
import com.stripe.android.PaymentConfiguration

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            BuildConfig.STRIPE_PUBLISHABLE_KEY
        )
    }
}