package com.example.trektimer.ui.tracking

import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class GeocodedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    viewModel: TrekViewModel,
    onExit: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var startLocationText by remember { mutableStateOf("") }
    var endLocationText by remember { mutableStateOf("") }
    
    var startLocation by remember { mutableStateOf<GeocodedLocation?>(null) }
    var endLocation by remember { mutableStateOf<GeocodedLocation?>(null) }
    
    var isGeocodingStart by remember { mutableStateOf(false) }
    var isGeocodingEnd by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Duration input
    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    
    val durationSeconds = remember(hoursText, minutesText) {
        val hours = hoursText.toIntOrNull() ?: 0
        val minutes = minutesText.toIntOrNull() ?: 0
        (hours * 3600L) + (minutes * 60L)
    }

    // Calculate distance when both locations are set
    val distance = remember(startLocation, endLocation) {
        if (startLocation != null && endLocation != null) {
            val result = FloatArray(1)
            Location.distanceBetween(
                startLocation!!.latitude, startLocation!!.longitude,
                endLocation!!.latitude, endLocation!!.longitude,
                result
            )
            result[0] / 1000.0 // Convert to km
        } else {
            null
        }
    }

    // Geocode function
    suspend fun geocodeLocation(query: String): GeocodedLocation? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var result: GeocodedLocation? = null
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        addresses.firstOrNull()?.let { addr ->
                            result = GeocodedLocation(
                                name = addr.getAddressLine(0) ?: query,
                                latitude = addr.latitude,
                                longitude = addr.longitude
                            )
                        }
                    }
                    // Small delay to allow callback
                    kotlinx.coroutines.delay(500)
                    result
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(query, 1)
                    addresses?.firstOrNull()?.let { addr ->
                        GeocodedLocation(
                            name = addr.getAddressLine(0) ?: query,
                            latitude = addr.latitude,
                            longitude = addr.longitude
                        )
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Trek Details") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Start Location
            OutlinedTextField(
                value = startLocationText,
                onValueChange = { 
                    startLocationText = it
                    startLocation = null
                },
                label = { Text("Start Location") },
                placeholder = { Text("e.g., Central Park, NYC") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
                trailingIcon = {
                    if (isGeocodingStart) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (startLocationText.isNotBlank()) {
                                    scope.launch {
                                        isGeocodingStart = true
                                        val result = geocodeLocation(startLocationText)
                                        isGeocodingStart = false
                                        if (result != null) {
                                            startLocation = result
                                            startLocationText = result.name
                                        } else {
                                            snackbarHostState.showSnackbar("Could not find location")
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        if (startLocationText.isNotBlank()) {
                            scope.launch {
                                isGeocodingStart = true
                                val result = geocodeLocation(startLocationText)
                                isGeocodingStart = false
                                if (result != null) {
                                    startLocation = result
                                    startLocationText = result.name
                                }
                            }
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Start location confirmation
            if (startLocation != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✓ ${startLocation!!.name}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // End Location
            OutlinedTextField(
                value = endLocationText,
                onValueChange = { 
                    endLocationText = it
                    endLocation = null
                },
                label = { Text("End Location") },
                placeholder = { Text("e.g., Times Square, NYC") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
                trailingIcon = {
                    if (isGeocodingEnd) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (endLocationText.isNotBlank()) {
                                    scope.launch {
                                        isGeocodingEnd = true
                                        val result = geocodeLocation(endLocationText)
                                        isGeocodingEnd = false
                                        if (result != null) {
                                            endLocation = result
                                            endLocationText = result.name
                                        } else {
                                            snackbarHostState.showSnackbar("Could not find location")
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        if (endLocationText.isNotBlank()) {
                            scope.launch {
                                isGeocodingEnd = true
                                val result = geocodeLocation(endLocationText)
                                isGeocodingEnd = false
                                if (result != null) {
                                    endLocation = result
                                    endLocationText = result.name
                                }
                            }
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // End location confirmation
            if (endLocation != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✓ ${endLocation!!.name}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duration Input
            Text(
                text = "Time Taken",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hoursText = it },
                    label = { Text("Hours") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text(":", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { 
                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                            val mins = it.toIntOrNull() ?: 0
                            if (mins <= 59) minutesText = it
                        }
                    },
                    label = { Text("Minutes") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Distance Display
            if (distance != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Estimated Distance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = String.format("%.2f km", distance),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "(straight-line distance)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (startLocation != null && endLocation != null && distance != null) {
                            scope.launch {
                                isSaving = true
                                viewModel.saveManualTrek(
                                    startName = startLocation!!.name,
                                    startLat = startLocation!!.latitude,
                                    startLng = startLocation!!.longitude,
                                    endName = endLocation!!.name,
                                    endLat = endLocation!!.latitude,
                                    endLng = endLocation!!.longitude,
                                    distanceKm = distance,
                                    durationSeconds = durationSeconds
                                )
                                isSaving = false
                                snackbarHostState.showSnackbar("Trek saved!")
                                onSaved()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = startLocation != null && endLocation != null && !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Trek")
                    }
                }
            }
        }
    }
}
