package com.comp3074_101384549.projectui.utils

import android.util.Base64
import org.json.JSONObject

object JwtPayloadUtil {

    fun readPayload(jwt: String): JSONObject? {
        val parts = jwt.split(".")
        if (parts.size < 2) return null
        val segment = parts[1]
        val padded = segment + "=".repeat((4 - segment.length % 4) % 4)
        return try {
            val json = String(Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
            JSONObject(json)
        } catch (_: Exception) {
            null
        }
    }
}
