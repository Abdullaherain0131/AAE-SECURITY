package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.util.ProtectionPreferences
import kotlinx.coroutines.*

/**
 * Ayrı proseste (`:watchdog`) çalışan diriltici.
 *
 * Önemli: Kullanıcı canlı kalkanı kapattığında bu servis de susmak zorunda. Eskiden
 * koşulsuz olarak 5 saniyede bir [RealTimeProtectionService]'i yeniden başlatıyordu,
 * bu yüzden kalkan hiçbir zaman kapatılamıyordu.
 */
class WatchdogService : Service() {
    private val watchdogJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + watchdogJob)
    private lateinit var prefs: ProtectionPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = ProtectionPreferences(this)
        startWatchdogLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as foreground to make it truly unkillable on modern Android
        try {
            val channelId = "aae_watchdog_service"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Core System Guard",
                    android.app.NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Core System Guard"
                    setShowBadge(false)
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.createNotificationChannel(channel)
            }

            val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("AAE Security")
                .setContentText("Sistem koruma gözlemcisi çalışıyor")
                .setSmallIcon(android.R.drawable.ic_secure)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {}

        // Kalkan kapalıysa hiç bekletmeden çekil.
        if (!prefs.isRealTimeProtectionEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startWatchdogLoop() {
        scope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)

                // Kullanıcı koruma kapattıysa döngüyü bitir ve kendini kapat.
                if (!prefs.isRealTimeProtectionEnabled) {
                    stopSelf()
                    return@launch
                }

                try {
                    // Servis zaten ayaktaysa boşuna startService çağırıp sistemi yormayalım.
                    if (RealTimeProtectionService.isServiceAlive(applicationContext)) continue

                    val mainIntent = Intent(applicationContext, RealTimeProtectionService::class.java)
                    mainIntent.action = RealTimeProtectionService.ACTION_START
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        applicationContext.startForegroundService(mainIntent)
                    } else {
                        applicationContext.startService(mainIntent)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogJob.cancel()
        scheduleSelfResurrection()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleSelfResurrection()
    }

    /** Yalnızca koruma açıkken kendini dirilt; aksi halde kapatma isteği anlamsızlaşıyordu. */
    private fun scheduleSelfResurrection() {
        if (!this::prefs.isInitialized) prefs = ProtectionPreferences(this)
        if (!prefs.isRealTimeProtectionEnabled) return

        try {
            val restartServiceIntent = Intent(applicationContext, this.javaClass)
            restartServiceIntent.setPackage(packageName)
            val restartServicePendingIntent = android.app.PendingIntent.getService(
                applicationContext, 1, restartServiceIntent,
                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmService.set(
                android.app.AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /** 1002 [com.example.util.NotificationHelper] tarafından da kullanılıyordu. */
        private const val NOTIFICATION_ID = 2004
        private const val CHECK_INTERVAL_MS = 30_000L
    }
}
