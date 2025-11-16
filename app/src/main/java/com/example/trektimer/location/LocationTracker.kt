package com.example.trektimer.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class LocationTracker(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    fun startTracking(onLocation: (Location) -> Unit) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onLocation(it) }
            }
        }

        client.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
    }

    fun stopTracking() {
        callback?.let { client.removeLocationUpdates(it) }
    }
}
