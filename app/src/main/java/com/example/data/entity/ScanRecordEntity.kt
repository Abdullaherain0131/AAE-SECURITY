package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanType: String, // "Hızlı Tarama", "Derin Tarama", "Gerçek Zamanlı"
    val timestamp: Long = System.currentTimeMillis(),
    val scannedAppsCount: Int,
    val threatsFoundCount: Int,
    val durationMs: Long
)
