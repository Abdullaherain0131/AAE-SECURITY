package com.example

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.service.ScheduledScanWorker
import java.util.concurrent.TimeUnit
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AntivirusDatabase
import com.example.data.AntivirusRepository

class AntivirusApplication : Application() {
    val database by lazy { AntivirusDatabase.getDatabase(this) }
    val repository by lazy { AntivirusRepository(database.antivirusDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        com.example.ai.AaeSecurityEngine.loadBundledModel(this, "singularity.tflite")
        com.example.service.DnsVpnService.startMonitoring(this)
        
        scheduleMalwareSync()
    }

    private fun scheduleMalwareSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
            
        val syncRequest = PeriodicWorkRequestBuilder<com.example.worker.MalwareSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MalwareSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Real-Time Protection Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_REALTIME_ID,
                "Gerçek Zamanlı Koruma Kalkanı",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Antivirüs gerçek zamanlı arka plan koruma durumu"
                setShowBadge(false)
            }

            // Threat Alerts Channel (High Importance)
            val threatChannel = NotificationChannel(
                CHANNEL_THREAT_ALERTS_ID,
                "Güvenlik Tehdit Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tespit edilen zararlı yazılım ve risk uyarıları"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(threatChannel)
        }
    }

    companion object {
        const val CHANNEL_REALTIME_ID = "channel_realtime_protection"
        const val CHANNEL_THREAT_ALERTS_ID = "channel_threat_alerts"

        lateinit var instance: AntivirusApplication
            private set
    }
}
