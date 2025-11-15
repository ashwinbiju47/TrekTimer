package com.example.trektimer

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp


class TrekTimerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Log.d("Trek Timer App", "Firebase initialized correctly")
    }
}
