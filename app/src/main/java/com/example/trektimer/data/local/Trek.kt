package com.example.trektimer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treks")
data class Trek(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String,
    val startTime: Long,       // epoch millis
    val endTime: Long,         // epoch millis
    val distanceKm: Double,
    val durationSeconds: Long,
    val avgSpeedMps: Double,
    val routeJson: String,     // JSON array of [lat, lng] points
    // Manual entry fields
    val isManualEntry: Boolean = false,
    val startLocationName: String? = null,
    val endLocationName: String? = null,
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null
)

