package com.example.trektimer

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.mapbox.mapboxsdk.Mapbox

class TrekTimerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Log.d("Trek Timer App", "Firebase initialized correctly")

        // MapLibre initialization
        Mapbox.getInstance(this)

        Log.d("Trek Timer App", "MapLibre initialized correctly")
    }
}
