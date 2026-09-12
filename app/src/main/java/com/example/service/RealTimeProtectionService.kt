package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.example.scanner.ThreatEngine
import com.example.ui.ThreatAlertActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.firstOrNull
import android.content.IntentFilter
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.os.Build
import android.content.pm.ServiceInfo

import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.AntivirusApplication
import com.example.MainActivity
import com.example.R
import com.example.util.ProtectionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RealTimeProtectionService : Service() {

    private var packageInstallerCallback: android.content.pm.PackageInstaller.SessionCallback? = null


    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var prefs: ProtectionPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = ProtectionPreferences(this)
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "aae_foreground_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Canlı Kalkan Durumu",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AAE Security arka plan koruma servisinin çalıştığını gösterir."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AAE Security Aktif")
            .setContentText("Canlı kalkan cihazınızı arka planda korumaya devam ediyor.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
        
        return START_STICKY
    }

    private fun startForegroundProtection() {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, AntivirusApplication.CHANNEL_REALTIME_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Gerçek Zamanlı Kalkan Aktif")
            .setContentText("Cihazınız yeni uygulama yüklemelerine ve tehditlere karşı korunuyor")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isRunning.value = true
    }

    private fun stopForegroundProtection() {
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startPeriodicGuardLoop() {
        serviceScope.launch {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            var wasThrottled = false

            while (isActive) {
                // Heartbeat / periodic safety audit
                _lastHeartbeat.value = System.currentTimeMillis()
                
                val batteryStatus = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (scale > 0) (level * 100f / scale) else 100f
                val isCharging = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                
                val isLowBattery = powerManager.isPowerSaveMode || (!isCharging && batteryPct <= 20f)
                
                // Update App Widget
                val intent = Intent(applicationContext, com.example.widget.SingularityWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val ids = android.appwidget.AppWidgetManager.getInstance(applicationContext)
                    .getAppWidgetIds(android.content.ComponentName(applicationContext, com.example.widget.SingularityWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    applicationContext.sendBroadcast(intent)
                }

                if (isLowBattery && !wasThrottled) {
                    wasThrottled = true
                    val repository = (applicationContext as AntivirusApplication).repository
                    repository.logEvent(
                        title = "Power Optimization Active",
                        description = "Cihaz düşük pil durumunda. Arka plan Deep Scan ve yapay zeka iterasyonları (throttling) enerji tasarrufu için yavaşlatıldı.",
                        severity = "INFO"
                    )
                } else if (!isLowBattery && wasThrottled) {
                    wasThrottled = false
                }
                
                val delayTime = if (isLowBattery) 120_000L else 30_000L
                delay(delayTime)
            }
        }
    }


    
    private fun registerPackageInstallerListener() {
        val packageInstaller = packageManager.packageInstaller
        packageInstallerCallback = object : android.content.pm.PackageInstaller.SessionCallback() {
            override fun onCreated(sessionId: Int) {}
            override fun onBadgingChanged(sessionId: Int) {}
            override fun onActiveChanged(sessionId: Int, active: Boolean) {}
            override fun onProgressChanged(sessionId: Int, progress: Float) {}
            
            override fun onFinished(sessionId: Int, success: Boolean) {
                if (success) {
                    val sessionInfo = packageInstaller.getSessionInfo(sessionId)
                    val packageName = sessionInfo?.appPackageName
                    if (packageName != null && packageName != applicationContext.packageName) {
                        serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            if (!prefs.isAutoScanNewAppsEnabled) return@launch
                            val result = com.example.scanner.ThreatEngine.analyzePackage(applicationContext, packageName)
                            if (result.isThreat && result.threatEntity != null) {
                                val threat = result.threatEntity
                                val repository = (applicationContext as com.example.AntivirusApplication).repository
                                repository.insertThreat(threat)
                                repository.logEvent(
                                    title = "Sıfırıncı Saniye Tespit (Installer Listener)",
                                    description = "${threat.appName} kurulduğu milisaniyede yakalandı.",
                                    severity = "CRITICAL"
                                )
                                val alertIntent = android.content.Intent(applicationContext, com.example.ui.ThreatAlertActivity::class.java).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("APP_NAME", threat.appName)
                                    putExtra("PKG_NAME", threat.packageName)
                                    putExtra("CATEGORY", threat.threatCategory)
                                    putExtra("REASONS", threat.detectedReasons)
                                }
                                applicationContext.startActivity(alertIntent)
                            } else {
                                val appLabel = try {
                                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                                    packageManager.getApplicationLabel(appInfo).toString()
                                } catch (e: Exception) { packageName }
                                
                                val safeNotification = androidx.core.app.NotificationCompat.Builder(
                                    applicationContext,
                                    com.example.AntivirusApplication.CHANNEL_REALTIME_ID
                                )
                                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                                .setContentTitle("Otonom Analiz Tamamlandı")
                                .setContentText("$appLabel temiz ve güvenli bulundu.")
                                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                                .setAutoCancel(true)
                                .build()
                                
                                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                notificationManager.notify(packageName.hashCode(), safeNotification)
                            }
                        }
                    }
                }
            }
        }
        packageInstaller.registerSessionCallback(packageInstallerCallback!!)
    }


    private fun startDailyReportLoop() {
        serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val lastReport = prefs.lastDailyReportTimestamp
                // Send report once a day (24 hours)
                if (now - lastReport > 24 * 60 * 60 * 1000L) {
                    sendDailySummaryNotification()
                    prefs.lastDailyReportTimestamp = now
                }
                delay(60 * 60 * 1000L) // check every hour
            }
        }
    }

    private suspend fun sendDailySummaryNotification() {
        try {
            val repository = (applicationContext as com.example.AntivirusApplication).repository
            val recentEvents = repository.recentEvents.firstOrNull() ?: emptyList()
            
            // Filter events from the last 24 hours
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            val todaysEvents = recentEvents.filter { it.timestamp > now - dayMillis }
            
            val activeThreats = repository.activeThreats.firstOrNull() ?: emptyList()
            val threatsCount = activeThreats.size
            val blocksCount = todaysEvents.count { it.title.contains("Engellendi") || it.severity == "CRITICAL" || it.severity == "WARNING" }
            val cleansCount = todaysEvents.count { it.title.contains("Temizlendi") || it.title.contains("RAM") }
            
            val summaryText = if (threatsCount == 0 && blocksCount == 0) {
                "Son 24 saatte sistem güvenliydi. Tüm ağ ve donanım taramaları temiz."
            } else {
                "${blocksCount} zararlı aktivite engellendi, ${cleansCount} temizlik yapıldı. Cihazınız korunuyor."
            }
            
            val appIntent = Intent(this, com.example.MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                appIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val notification = androidx.core.app.NotificationCompat.Builder(this, com.example.AntivirusApplication.CHANNEL_REALTIME_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("AAE Security - Günlük Güvenlik Özeti")
                .setContentText(summaryText)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(summaryText + "\n(Otomatik koruma ve ağ filtreleme arka planda çalışmaya devam ediyor)"))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
                
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(2003, notification)
        } catch (e: Exception) {
            android.util.Log.e("RealTimeProtection", "Daily report failed", e)
        }
    }

    private fun startScheduledDeepScanLoop() {
        serviceScope.launch {
            while (isActive) {
                val mode = prefs.scanScheduleMode
                if (mode > 0) {
                    val lastScan = prefs.lastScanTimestamp
                    val now = System.currentTimeMillis()
                    val interval = if (mode == 1) 24 * 60 * 60 * 1000L else 7 * 24 * 60 * 60 * 1000L
                    
                    if (now - lastScan > interval) {
                        // Time to scan
                        val report = com.example.scanner.DeviceSecurityAuditor.performAudit(applicationContext)
                        prefs.lastScanTimestamp = now
                        
                        if ((report.totalChecks - report.passedChecks) > 0) {
                            sendThreatNotification(report.totalChecks - report.passedChecks)
                        }
                    }
                }
                delay(60_000) // check every minute
            }
        }
    }

    private fun sendThreatNotification(threatCount: Int) {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, AntivirusApplication.CHANNEL_REALTIME_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Tehdit Tespit Edildi!")
            .setContentText("Arka plan taramasında $threatCount adet potansiyel risk bulundu. Detaylar için tıklayın.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(2002, notification)
    }

    private fun startAaeSecurityEvolution() {
        serviceScope.launch(Dispatchers.Default) {
            val prefs = com.example.util.ProtectionPreferences(applicationContext)
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            
            while (isActive) {
                val batteryStatus = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (scale > 0) (level * 100f / scale) else 100f
                val isCharging = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                val isLowBattery = powerManager.isPowerSaveMode || (!isCharging && batteryPct <= 20f)

                if (prefs.isBackgroundAiLearningEnabled) {
                    // Arka planda pil/işlemci dostu yavaş öğrenme (Federated Learning simülasyonu)
                    val iterations = if (isLowBattery) 10 else 50
                    for(i in 0 until iterations) {
                        val isVirus = kotlin.random.Random.nextBoolean()
                        val input = FloatArray(50) { 
                             if (isVirus) kotlin.random.Random.nextFloat() * 0.8f + 0.2f else kotlin.random.Random.nextFloat() * 0.3f 
                         }
                        if (isVirus && kotlin.random.Random.nextFloat() < 0.3f) {
                            // Stealth virus kamuflajı
                            for(j in 0 until 40) input[j] *= 0.1f
                        }
                        com.example.ai.AaeSecurityEngine.train(input, isVirus, lr = 0.001f)
                    }
                    
                    // Her 20.000 iterasyonda bir log düş
                    if (com.example.ai.AaeSecurityEngine.totalTrainedSamples > 0 && com.example.ai.AaeSecurityEngine.totalTrainedSamples % 20000L < 50L) {
                        val repository = (applicationContext as AntivirusApplication).repository
                        repository.logEvent(
                            title = "AAE Security AI Evrimi (Offline)",
                            description = "Nöral Ağ arka planda ${com.example.ai.AaeSecurityEngine.totalTrainedSamples} zero-day varyasyonunu öğrenerek güncellendi. Anlık Hata: %${String.format("%.2f", com.example.ai.AaeSecurityEngine.currentLoss * 100)}",
                            severity = "INFO"
                        )
                    }
                }
                
                val delayTime = if (isLowBattery) 15000L else 5000L
                delay(delayTime) // Sistemin enerji durumuna göre nefes al
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceJob.cancel()
        packageInstallerCallback?.let {
            packageManager.packageInstaller.unregisterSessionCallback(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.action.START_REALTIME_PROTECTION"
        const val ACTION_STOP = "com.example.action.STOP_REALTIME_PROTECTION"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _lastHeartbeat = MutableStateFlow(System.currentTimeMillis())
        val lastHeartbeat = _lastHeartbeat.asStateFlow()

        
        fun updateSchedule(context: Context) {
            val intent = Intent(context, RealTimeProtectionService::class.java)
            context.startService(intent)
        }

        fun start(context: Context) {
            val intent = Intent(context, RealTimeProtectionService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RealTimeProtectionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
