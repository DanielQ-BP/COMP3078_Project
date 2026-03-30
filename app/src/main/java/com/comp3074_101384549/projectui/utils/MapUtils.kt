package com.comp3074_101384549.projectui.utils

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import java.io.IOException

object MapUtils {

    fun addMarker(
        map: GoogleMap,
        position: LatLng,
        title: String,
        snippet: String? = null
    ): Marker? {
        return map.addMarker(
            MarkerOptions()
                .position(position)
                .title(title)
                .snippet(snippet)
        )
    }

    fun drawRoute(
        map: GoogleMap,
        points: List<LatLng>,
        color: Int = android.graphics.Color.BLUE,
        width: Float = 10f
    ): Polyline? {
        if (points.isEmpty()) return null

        return map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(color)
                .width(width)
        )
    }

    fun moveCameraToPosition(
        map: GoogleMap,
        position: LatLng,
        zoom: Float = 15f,
        animate: Boolean = true
    ) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, zoom)
        if (animate) {
            map.animateCamera(cameraUpdate)
        } else {
            map.moveCamera(cameraUpdate)
        }
    }

    fun fitBounds(
        map: GoogleMap,
        points: List<LatLng>,
        padding: Int = 100,
        animate: Boolean = true
    ) {
        if (points.isEmpty()) return

        val builder = LatLngBounds.Builder()
        points.forEach { builder.include(it) }
        val bounds = builder.build()

        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        if (animate) {
            map.animateCamera(cameraUpdate)
        } else {
            map.moveCamera(cameraUpdate)
        }
    }

    fun getLatLngFromAddress(context: Context, address: String): LatLng? {
        return try {
            val geocoder = Geocoder(context)
            val addresses: List<Address>? = geocoder.getFromLocationName(address, 1)
            if (addresses?.isNotEmpty() == true) {
                val location = addresses[0]
                LatLng(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun launchGoogleMapsNavigation(context: Context, destination: LatLng) {
        val uri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val browserUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    fun launchGoogleMapsNavigation(context: Context, address: String) {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(address)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val browserUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(address)}"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }
}
