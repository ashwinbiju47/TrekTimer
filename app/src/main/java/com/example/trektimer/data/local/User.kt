package com.example.trektimer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val firebaseUid: String,
    val email: String
)
