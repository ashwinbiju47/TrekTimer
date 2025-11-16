package com.example.trektimer.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import com.example.trektimer.data.local.User

@Composable
fun HomeScreen(
    user: User,
    onLogout: () -> Unit,
    onStartTracking: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // global padding
    ) {

        // Top bar: Greeting + Logout icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hi, ${user.email}",
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout"
                )
            }
        }

        // Center button
        Button(
            onClick = onStartTracking,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text("Start Trek Tracking")
        }
    }
}
