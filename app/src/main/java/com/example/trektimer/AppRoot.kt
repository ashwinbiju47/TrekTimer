package com.example.trektimer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trektimer.data.local.User
import com.example.trektimer.ui.auth.AuthScreen
import com.example.trektimer.ui.auth.AuthViewModel
import com.example.trektimer.ui.home.HomeScreen
import com.example.trektimer.ui.home.TrekModeDialog
import com.example.trektimer.ui.splash.SplashScreen
import com.example.trektimer.ui.tracking.ManualEntryScreen
import com.example.trektimer.ui.tracking.TrackingScreen
import com.example.trektimer.ui.tracking.TrekViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = viewModel()
    val trekViewModel: TrekViewModel = viewModel()

    val auth = FirebaseAuth.getInstance()
    var loggedInUser by remember { mutableStateOf<User?>(null) }
    var showSplash by remember { mutableStateOf(true) }
    var showTracking by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission state
    var pendingTrackingStart by remember { mutableStateOf(false) }

    // Permission launcher for foreground location
    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            // For Android 10+, also request background location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pendingTrackingStart = true
            } else {
                showTracking = true
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission required to track your trek")
            }
        }
    }

    // Separate launcher for background location (Android 10+)
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Proceed regardless - background is optional but nice to have
        showTracking = true
    }

    // Handle pending background permission request
    LaunchedEffect(pendingTrackingStart) {
        if (pendingTrackingStart && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pendingTrackingStart = false
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // Check if has location permission
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request permissions and start GPS tracking
    fun requestPermissionsAndStartTracking() {
        if (hasLocationPermission()) {
            showTracking = true
        } else {
            foregroundPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        showSplash = false

        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val localUser = authViewModel.getLocalUser(firebaseUser.uid)
            loggedInUser = localUser
        }
    }

    // Set user UID on TrekViewModel when logged in
    LaunchedEffect(loggedInUser) {
        loggedInUser?.let {
            trekViewModel.currentUserUid = it.firebaseUid
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut()
        ) {
            SplashScreen()
        }

        if (!showSplash) {
            // Not logged in → auth flow
            if (loggedInUser == null) {
                AuthScreen(onAuthSuccess = { loggedInUser = it })
            } else {
                // Logged in → route control
                when {
                    showTracking -> {
                        TrackingScreen(
                            viewModel = trekViewModel,
                            onExit = { showTracking = false }
                        )
                    }
                    showManualEntry -> {
                        ManualEntryScreen(
                            viewModel = trekViewModel,
                            onExit = { showManualEntry = false },
                            onSaved = { showManualEntry = false }
                        )
                    }
                    else -> {
                        val treks by trekViewModel.treks.collectAsStateWithLifecycle()
                        HomeScreen(
                            user = loggedInUser!!,
                            treks = treks,
                            onLogout = {
                                authViewModel.logout()
                                loggedInUser = null
                            },
                            onStartTracking = { showModeDialog = true }
                        )
                    }
                }
            }
        }

        // Trek Mode Selection Dialog
        if (showModeDialog) {
            TrekModeDialog(
                onDismiss = { showModeDialog = false },
                onGpsSelected = { requestPermissionsAndStartTracking() },
                onManualSelected = { showManualEntry = true }
            )
        }

        // Snackbar for permission messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}