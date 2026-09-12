package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AntivirusDatabase
import com.example.data.entity.ScanRecordEntity
import com.example.scanner.ThreatEngine


class ScheduledScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = com.example.data.AntivirusDatabase.getDatabase(applicationContext)
            val pm = applicationContext.packageManager
            
            // OOM Korumalı Stream Ayrıştırma simülasyonu ve yüklü paket taraması
            val packages = pm.getInstalledPackages(0).take(20) // Pil koruması için limitli arka plan taraması
            val newlyFoundThreats = mutableListOf<com.example.data.entity.ThreatEntity>()
            
            for (pkg in packages) {
                val result = ThreatEngine.evaluatePackageInfo(applicationContext, pkg)
                if (result.isThreat && result.threatEntity != null) {
                    newlyFoundThreats.add(result.threatEntity)
                    db.antivirusDao().insertThreat(result.threatEntity)
                }
            }
            
            // Log to DB
            val record = ScanRecordEntity(
                scanType = "Otonom Arka Plan Taraması",
                timestamp = System.currentTimeMillis(),
                scannedAppsCount = packages.size,
                threatsFoundCount = newlyFoundThreats.size,
                durationMs = 500 // Minimal simulated time
            )
            db.antivirusDao().insertScanRecord(record)
            
            if (newlyFoundThreats.isNotEmpty()) {
                com.example.util.NotificationHelper.showScanCompleteNotification(applicationContext, newlyFoundThreats.size)
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
