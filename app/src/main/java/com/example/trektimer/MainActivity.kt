package com.example.trektimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.trektimer.ui.AppRoot
import com.example.trektimer.ui.theme.TrekTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrekTimerTheme(
                darkTheme = false,   // or true / isSystemInDarkTheme()
                dynamicColor = false // disable wallpaper colors
            ) {
                AppRoot()
            }
        }
    }
}
