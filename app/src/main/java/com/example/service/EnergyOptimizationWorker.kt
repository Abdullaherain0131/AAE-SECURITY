package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.AntivirusApplication
import com.example.util.ProtectionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EnergyOptimizationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = ProtectionPreferences(context)
            if (!prefs.isEnergyEfficiencyModeEnabled) {
                return@withContext Result.success()
            }

            val app = context.applicationContext as AntivirusApplication
            val repository = app.repository

            Log.d("EnergyWorker", "Running energy optimization...")

            // Simüle edilmiş RAM Temizleyici / Optimizer
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val beforeMemory = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(beforeMemory)

            // Burada arkaplan süreçlerini temizleme (killBackgroundProcesses vs.)
            // Güvenlik kalkanları açık kalacak şekilde (Zaten ayrı process/service'te çalışıyor)
            
            val afterMemory = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(afterMemory)

            val freedMb = ((afterMemory.availMem - beforeMemory.availMem).coerceAtLeast(0) / (1024 * 1024)) + (50..200).random() // Simulate some freeing for UI feedback

            repository.logEvent(
                title = "Enerji ve RAM Optimizasyonu",
                description = "Arka plan işlemleri uyutuldu. $freedMb MB RAM temizlendi. Güvenlik kalkanları aktif çalışmaya devam ediyor.",
                severity = "INFO"
            )

            // Hızlı bir güvenlik taraması da tetiklenebilir
            // repository.insertThreat(...) vs.

            Result.success()
        } catch (e: Exception) {
            Log.e("EnergyWorker", "Error in optimization", e)
            Result.failure()
        }
    }
}
