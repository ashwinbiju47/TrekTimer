package com.example.trektimer.ui.tracking

import android.location.Location
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.geometry.LatLng

class TrekViewModel : ViewModel() {

    private val _points = mutableStateListOf<LatLng>()
    val points: List<LatLng> = _points

    var distance by mutableDoubleStateOf(0.0)
    var speed by mutableDoubleStateOf(0.0)

    fun addPoint(location: Location) {
        val newP = LatLng(location.latitude, location.longitude)

        if (_points.isNotEmpty()) {
            val prev = _points.last()
            distance += calculateDistance(prev, newP)
        }

        speed = location.speed.toDouble()
        _points.add(newP)
    }

    private fun calculateDistance(p1: LatLng, p2: LatLng): Double {
        val result = FloatArray(1)
        Location.distanceBetween(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
            result
        )
        return result[0] / 1000.0   // convert m -> km
    }
}
