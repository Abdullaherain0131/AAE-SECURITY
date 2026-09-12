package com.example.workers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.scanner.ThreatEngine
import com.example.data.AppDatabase
import com.example.data.ScanReportRepository
import com.example.util.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PeriodicScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("PeriodicScanWorker", "Starting periodic scan...")
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = ScanReportRepository(db.scanReportDao(), db.threatLogDao(), db.ignoredAppDao())
            
            val threatEngine = ThreatEngine(applicationContext)
            
            val pm = applicationContext.packageManager
            val packages = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                }
            } catch (e: Exception) {
                emptyList()
            }
            
            var totalThreats = 0
            
            packages.forEach { app ->
                val result = threatEngine.analyzePackage(applicationContext, app.packageName)
                if (result.isThreat) {
                    totalThreats++
                }
            }
            
            if (totalThreats > 0) {
                NotificationUtils.showThreatFoundNotification(applicationContext, totalThreats)
            }
            
            Log.d("PeriodicScanWorker", "Periodic scan completed. Threats found: $totalThreats")
            
            val prefs = com.example.util.ProtectionPreferences(applicationContext)
            if (prefs.scanScheduleMode > 0) {
                ScanWorkerHelper.scheduleScan(applicationContext, prefs.scanTargetHour, prefs.scanTargetMinute)
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("PeriodicScanWorker", "Error during periodic scan", e)
            Result.failure()
        }
    }
}
