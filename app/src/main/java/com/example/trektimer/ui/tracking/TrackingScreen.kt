package com.example.trektimer.ui.tracking

import android.location.Location
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    Column(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.weight(1f),
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

        SummaryCard(distance = viewModel.distance, speed = viewModel.speed)
        var isTracking by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Button(
                onClick = {
                    isTracking = true
                    tracker.startTracking { loc -> viewModel.addPoint(loc) }
                },
                enabled = !isTracking
            ) {
                Text("Start")
            }

            Button(
                onClick = {
                    isTracking = false
                    tracker.stopTracking()
                },
                enabled = isTracking
            ) {
                Text("Stop")
            }

            Button(onClick = onExit) {
                Text("Back")
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


