package com.example.trektimer.ui.tracking

import android.app.Application
import android.location.Location
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trektimer.data.local.AppDatabase
import com.example.trektimer.data.local.Trek
import com.example.trektimer.data.repository.TrekRepository
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray

class TrekViewModel(application: Application) : AndroidViewModel(application) {

    private val trekRepository = TrekRepository(
        AppDatabase.getDatabase(application).trekDao()
    )

    // Route points
    private val _points = mutableStateListOf<LatLng>()
    val points: List<LatLng> = _points

    // Statistics
    var distance by mutableDoubleStateOf(0.0)
        private set
    var speed by mutableDoubleStateOf(0.0)
        private set

    // Elapsed time
    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    // Tracking state
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // Timer job
    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L

    // Current user UID (set from AppRoot)
    private val _currentUserUid = MutableStateFlow("")
    var currentUserUid: String
        get() = _currentUserUid.value
        set(value) { _currentUserUid.value = value }

    // Treks for current user (reactive)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val treks: StateFlow<List<Trek>> = _currentUserUid
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                trekRepository.getTreksForUser(uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startTracking() {
        if (_isTracking.value) return

        // Reset state for new trek
        _points.clear()
        distance = 0.0
        speed = 0.0
        _elapsedTimeSeconds.value = 0L
        startTimeMillis = System.currentTimeMillis()
        _isTracking.value = true

        // Start timer
        timerJob = viewModelScope.launch {
            while (isActive && _isTracking.value) {
                delay(1000L)
                _elapsedTimeSeconds.value += 1
            }
        }
    }

    fun stopTracking() {
        if (!_isTracking.value) return

        _isTracking.value = false
        timerJob?.cancel()
        timerJob = null

        // Save trek if we have points
        if (_points.size >= 2) {
            saveTrek()
        }
    }

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

    private fun saveTrek() {
        if (currentUserUid.isEmpty()) return

        val endTime = System.currentTimeMillis()
        val durationSeconds = _elapsedTimeSeconds.value
        val avgSpeed = if (durationSeconds > 0) {
            (distance * 1000) / durationSeconds  // m/s
        } else {
            0.0
        }

        // Convert points to JSON
        val routeJson = JSONArray().apply {
            _points.forEach { point ->
                put(JSONArray().apply {
                    put(point.latitude)
                    put(point.longitude)
                })
            }
        }.toString()

        val trek = Trek(
            userUid = currentUserUid,
            startTime = startTimeMillis,
            endTime = endTime,
            distanceKm = distance,
            durationSeconds = durationSeconds,
            avgSpeedMps = avgSpeed,
            routeJson = routeJson
        )

        viewModelScope.launch {
            trekRepository.saveTrek(trek)
        }
    }

    fun saveManualTrek(
        startName: String,
        startLat: Double,
        startLng: Double,
        endName: String,
        endLat: Double,
        endLng: Double,
        distanceKm: Double,
        durationSeconds: Long = 0L
    ) {
        if (currentUserUid.isEmpty()) return

        val now = System.currentTimeMillis()
        
        // Calculate average speed (m/s) if duration provided
        val avgSpeed = if (durationSeconds > 0) {
            (distanceKm * 1000) / durationSeconds
        } else {
            0.0
        }
        
        // Create route JSON with just start and end points
        val routeJson = JSONArray().apply {
            put(JSONArray().apply { put(startLat); put(startLng) })
            put(JSONArray().apply { put(endLat); put(endLng) })
        }.toString()

        val trek = Trek(
            userUid = currentUserUid,
            startTime = now,
            endTime = now + (durationSeconds * 1000),
            distanceKm = distanceKm,
            durationSeconds = durationSeconds,
            avgSpeedMps = avgSpeed,
            routeJson = routeJson,
            isManualEntry = true,
            startLocationName = startName,
            endLocationName = endName,
            startLat = startLat,
            startLng = startLng,
            endLat = endLat,
            endLng = endLng
        )

        viewModelScope.launch {
            trekRepository.saveTrek(trek)
        }
    }

    fun clearTrek() {
        _points.clear()
        distance = 0.0
        speed = 0.0
        _elapsedTimeSeconds.value = 0L
        _isTracking.value = false
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

