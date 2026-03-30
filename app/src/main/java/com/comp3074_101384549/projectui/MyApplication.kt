package com.comp3074_101384549.projectui

import android.app.Application
import com.stripe.android.PaymentConfiguration

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51SXchzK6Clah5kX0Wtj1y8q37txVKfMvWqhxPITIlfOxqTipWg91IhezuR547gQOJwIgNSKm8ekZ9E8y3nEWOFii00BEoXvvyy"
        )
    }
}