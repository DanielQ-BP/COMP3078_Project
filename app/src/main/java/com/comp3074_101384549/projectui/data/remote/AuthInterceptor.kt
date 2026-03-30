package com.comp3074_101384549.projectui.data.remote

import com.comp3074_101384549.projectui.data.local.AuthPreferences // CORRECTED IMPORT
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that adds the Authorization header with the user's token
 * to all outgoing API requests.
 */
class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Synchronously get the token from DataStore for the network request
        val token = runBlocking {
            authPreferences.authToken.first()
        }

        // Build a new request with the Authorization header if the token exists
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}