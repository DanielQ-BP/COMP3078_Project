package com.comp3074_101384549.projectui.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class DirectionsResult(
    val polylinePoints: List<LatLng>,
    val distance: String,
    val duration: String
)

class DirectionsApiHelper {
    companion object {
        private const val TAG = "DirectionsApiHelper"
        private const val DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json"

        suspend fun getDirections(
            origin: LatLng,
            destination: LatLng,
            apiKey: String
        ): DirectionsResult? = withContext(Dispatchers.IO) {
            try {
                val url = buildDirectionsUrl(origin, destination, apiKey)
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    parseDirectionsResponse(response)
                } else {
                    Log.e(TAG, "API request failed with code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting directions", e)
                null
            }
        }

        private fun buildDirectionsUrl(origin: LatLng, destination: LatLng, apiKey: String): String {
            return "$DIRECTIONS_API_URL?" +
                    "origin=${origin.latitude},${origin.longitude}&" +
                    "destination=${destination.latitude},${destination.longitude}&" +
                    "key=$apiKey"
        }

        private fun parseDirectionsResponse(jsonResponse: String): DirectionsResult? {
            try {
                val json = JSONObject(jsonResponse)
                val status = json.getString("status")

                if (status != "OK") {
                    Log.e(TAG, "Directions API error: $status")
                    return null
                }

                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) return null

                val route = routes.getJSONObject(0)
                val polyline = route.getJSONObject("overview_polyline").getString("points")
                val legs = route.getJSONArray("legs").getJSONObject(0)
                val distance = legs.getJSONObject("distance").getString("text")
                val duration = legs.getJSONObject("duration").getString("text")

                val points = decodePolyline(polyline)

                return DirectionsResult(points, distance, duration)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing directions response", e)
                return null
            }
        }

        private fun decodePolyline(encoded: String): List<LatLng> {
            val poly = ArrayList<LatLng>()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0

            while (index < len) {
                var b: Int
                var shift = 0
                var result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng

                val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
                poly.add(latLng)
            }

            return poly
        }
    }
}
