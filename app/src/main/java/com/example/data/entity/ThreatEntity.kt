package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val threatCategory: String, // e.g., "SMS Ele Geçirme / Spyware", "Ekran Katmanı / Overlay", "Aşırı Yetki"
    val riskLevel: String, // "KRİTİK", "YÜKSEK", "ORTA", "DÜŞÜK"
    val riskScore: Int, // 0 - 100
    val detectedReasons: String, // Pipe or comma separated reasons
    val detectedAt: Long = System.currentTimeMillis(),
    val status: String = "AKTİF" // "AKTİF", "ÇÖZÜLDÜ", "GÜVENLİ_LİSTE"
)
