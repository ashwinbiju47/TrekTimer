package com.example.trektimer.ui

import androidx.compose.runtime.*
import com.example.trektimer.data.local.User
import com.example.trektimer.ui.auth.AuthScreen
import com.example.trektimer.ui.home.HomeScreen

@Composable
fun AppRoot() {
    var loggedInUser by remember { mutableStateOf<User?>(null) }

    if (loggedInUser == null) {
        AuthScreen(onAuthSuccess = { user -> loggedInUser = user })
    } else {
        HomeScreen(user = loggedInUser!!, onLogout = { loggedInUser = null })
    }
}
