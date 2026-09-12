package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_events")
data class SecurityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val severity: String = "INFO" // "INFO", "WARNING", "CRITICAL", "SUCCESS"
)
