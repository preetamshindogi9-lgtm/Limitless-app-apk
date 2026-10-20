package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 30, // 30 minutes default
    val isEnabled: Boolean = true,
    val category: String = "Social",
    val usedMinutesToday: Int = 0,
    val lastResetDate: String = "" // YYYY-MM-DD
)
