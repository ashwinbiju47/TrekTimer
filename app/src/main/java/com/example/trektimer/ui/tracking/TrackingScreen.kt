package com.example.trektimer.ui.tracking

import android.location.Location
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.trektimer.location.LocationTracker
import com.example.trektimer.map.MapConfig

// MapLibre uses Mapbox package names internally
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point

@Composable
fun TrackingScreen(
    viewModel: TrekViewModel,
    tracker: LocationTracker,
    onExit: () -> Unit
) {
    var mapView: MapView? by remember { mutableStateOf(null) }
    var mapLibre: MapboxMap? by remember { mutableStateOf(null) }
    var isTracking by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Full Screen Map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    onCreate(Bundle())
                    getMapAsync { map ->
                        mapLibre = map
                        map.setStyle(Style.Builder().fromUri(MapConfig.STYLE_URL)) {
                            if (viewModel.points.isNotEmpty()) {
                                val last = viewModel.points.last()
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(last, 15.0)
                                )
                            }
                        }
                    }
                    mapView = this
                }
            },
            update = {
                drawPolyline(mapLibre, viewModel.points)
            }
        )

        // 2. Top Stats Overlay
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = viewModel.distance.format(2),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = viewModel.speed.format(1),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "m/s",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 3. Bottom Control Panel
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Exit
                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Exit")
                }

                // Start / Stop Action
                Button(
                    onClick = {
                        if (isTracking) {
                            isTracking = false
                            tracker.stopTracking()
                        } else {
                            isTracking = true
                            tracker.startTracking { loc -> viewModel.addPoint(loc) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(56.dp)
                        .width(120.dp)
                ) {
                    Text(
                        text = if (isTracking) "STOP" else "START",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    distance: Double,
    speed: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Distance: ${distance.format(2)} km")
            Text("Speed: ${speed.format(2)} m/s")
        }
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)


// Draw GPS path on MapLibre
fun drawPolyline(map: MapboxMap?, points: List<LatLng>) {
    if (map == null || points.size < 2) return

    val source = GeoJsonSource(
        "route-source",
        LineString.fromLngLats(
            points.map { Point.fromLngLat(it.longitude, it.latitude) }
        )
    )

    val lineLayer = LineLayer("route-layer", "route-source")
        .withProperties(
            PropertyFactory.lineColor("#FF0000"),
            PropertyFactory.lineWidth(4f)
        )

    map.style?.apply {
        removeLayer("route-layer")
        removeSource("route-source")
        addSource(source)
        addLayer(lineLayer)
    }
}


