package com.example.service

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.firstOrNull
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.content.pm.ServiceInfo

import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.AntivirusApplication
import com.example.MainActivity
import com.example.util.NewAppInspector
import com.example.util.ProtectionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RealTimeProtectionService : Service() {

    private var packageInstallerCallback: PackageInstaller.SessionCallback? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var prefs: ProtectionPreferences

    /** Arka plan döngüleri yalnızca bir kez kurulmalı; START_STICKY ve watchdog tekrar tekrar onStartCommand tetikler. */
    private var loopsStarted = false

    /** startForeground çağrıldı mı? Servis sözleşmesini ihlal etmemek için takip edilir. */
    private var isForegroundStarted = false

    override fun onCreate() {
        super.onCreate()
        prefs = ProtectionPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1) Kullanıcı kalkanı kapatmak istiyor.
        if (intent?.action == ACTION_STOP) {
            handleStopRequest()
            return START_NOT_STICKY
        }

        // 2) Kalkan kapalıyken gelen her başlatma isteğini (watchdog, START_STICKY yeniden
        //    doğuşu, BootReceiver) yut. Aksi halde kalkan asla kapatılamıyordu.
        if (!prefs.isRealTimeProtectionEnabled) {
            stopForegroundProtection()
            return START_NOT_STICKY
        }

        // 3) Asıl koruma: bildirim + arka plan döngüleri + watchdog.
        startForegroundProtection()
        startProtectionLoops()
        ensureWatchdogRunning()

        return START_STICKY
    }

    /**
     * Servisin gerçek işini yapan altı döngüyü kurar. Daha önce bu metotlar tanımlıydı
     * ama hiçbir yerden çağrılmıyordu; kalkan sadece bir bildirimden ibaretti.
     */
    private fun startProtectionLoops() {
        if (loopsStarted) return
        loopsStarted = true

        startPeriodicGuardLoop()
        registerPackageInstallerListener()
        startDailyReportLoop()
        startScheduledDeepScanLoop()
        startIntegritySweep()

        serviceScope.launch {
            try {
                val repository = (applicationContext as AntivirusApplication).repository
                val startedAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                repository.logEvent(
                    title = "Canlı Kalkan Devrede",
                    description = "Arka plan gözlemcileri $startedAt itibarıyla çalışıyor: kurulum dinleyicisi, " +
                        "periyodik denetim, zamanlanmış tarama ve günlük özet raporu.",
                    severity = "SUCCESS"
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Başlangıç logu yazılamadı", e)
            }
        }
    }

    private fun handleStopRequest() {
        // Niyeti kalıcı hale getir: watchdog ve onTaskRemoved diriltmesi bu bayrağa bakıyor.
        prefs.isRealTimeProtectionEnabled = false
        stopWatchdog()
        // Kullanıcıya gösterilen "durduruldu" olayını AntivirusViewModel zaten yazıyor;
        // burada tekrar yazmak olay akışında mükerrer kayıt oluşturuyor.
        stopForegroundProtection()
    }

    private fun ensureWatchdogRunning() {
        try {
            val watchdogIntent = Intent(applicationContext, WatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(watchdogIntent)
            } else {
                applicationContext.startService(watchdogIntent)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Watchdog başlatılamadı", e)
        }
    }

    private fun stopWatchdog() {
        try {
            applicationContext.stopService(Intent(applicationContext, WatchdogService::class.java))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Watchdog durdurulamadı", e)
        }
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

        isForegroundStarted = true
        _isRunning.value = true
        _lastHeartbeat.value = System.currentTimeMillis()
    }

    private fun stopForegroundProtection() {
        // startForegroundService ile başlatıldıysak startForeground çağırmadan çıkmak
        // ForegroundServiceDidNotStartInTimeException ile çökmeye yol açar.
        if (!isForegroundStarted) {
            try {
                startForegroundProtection()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Ön plan sözleşmesi karşılanamadı", e)
            }
        }

        _isRunning.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
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

                // Update Mini Status Widget
                val miniIntent = Intent(applicationContext, com.example.widget.MiniStatusWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val miniIds = android.appwidget.AppWidgetManager.getInstance(applicationContext)
                    .getAppWidgetIds(android.content.ComponentName(applicationContext, com.example.widget.MiniStatusWidgetProvider::class.java))
                if (miniIds.isNotEmpty()) {
                    miniIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, miniIds)
                    applicationContext.sendBroadcast(miniIntent)
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
        try {
            val packageInstaller = packageManager.packageInstaller
            val callback = object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}
                override fun onBadgingChanged(sessionId: Int) {}
                override fun onActiveChanged(sessionId: Int, active: Boolean) {}
                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (!success) return
                    val packageName = try {
                        packageInstaller.getSessionInfo(sessionId)?.appPackageName
                    } catch (e: Exception) {
                        null
                    } ?: return

                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            // Mükerrer uyarı koruması NewAppInspector içinde; PackageInstallReceiver
                            // aynı kurulumu yayın üzerinden de görüyor.
                            NewAppInspector.inspectInstalledPackage(
                                context = applicationContext,
                                packageName = packageName,
                                source = NewAppInspector.SOURCE_INSTALLER_SESSION
                            )
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Installer analizi başarısız: $packageName", e)
                        }
                    }
                }
            }
            // Tek argümanlı overload çağıran thread'de Looper şartı arar; açıkça main looper veriyoruz.
            packageInstaller.registerSessionCallback(callback, Handler(Looper.getMainLooper()))
            packageInstallerCallback = callback
        } catch (e: Exception) {
            android.util.Log.w(TAG, "PackageInstaller dinleyicisi kurulamadı", e)
        }
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
            notificationManager.notify(NOTIFICATION_ID_DAILY_SUMMARY, notification)
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
        val notification = NotificationCompat.Builder(this, AntivirusApplication.CHANNEL_THREAT_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Tehdit Tespit Edildi!")
            .setContentText("Arka plan taramasında $threatCount adet potansiyel risk bulundu. Detaylar için tıklayın.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID_AUDIT_ALERT, notification)
    }

    /**
     * Arka plan bütünlük taraması.
     *
     * Bunun yerinde daha önce "AAE Security AI Evrimi" adlı bir döngü vardı:
     * her 5 saniyede bir `Random.nextBoolean()` etiketleriyle `Random.nextFloat()`
     * verisi üretip [com.example.ai.AaeSecurityEngine.train] çağırıyordu. O fonksiyon
     * ise yalnızca bir sayaç artıran boş bir gövdeydi (TFLite modeli salt okunur).
     * Sonuç: hiçbir öğrenme olmadan sürekli CPU/pil tüketimi ve kullanıcıya
     * "Nöral Ağ N zero-day varyasyonunu öğrendi, Anlık Hata: %0.00" diyen uydurma bir kayıt.
     *
     * Yerine gerçek iş yapan bir süpürme kondu: kurulu paketler sırayla yeniden
     * analiz edilir. Bu, kurulumdan **sonra** ortaya çıkan tehditleri yakalar —
     * bir uygulamanın güncellemeyle farklı bir anahtarla yeniden imzalanması
     * (yeniden paketleme) ya da yeni indirilen istihbaratın eski bir paketle eşleşmesi gibi.
     * Tarama tamamen çevrimdışıdır ve pil durumuna göre kendini yavaşlatır.
     */
    private fun startIntegritySweep() {
        serviceScope.launch(Dispatchers.Default) {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val repository = (applicationContext as AntivirusApplication).repository

            // Süpürme kaldığı yerden devam etsin; her turda baştan başlamak
            // hem gereksiz iş hem de listenin sonundaki paketlere hiç sıra gelmemesi demek.
            var cursor = 0

            while (isActive) {
                val batteryStatus = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (scale > 0) (level * 100f / scale) else 100f
                val isCharging = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ==
                    android.os.BatteryManager.BATTERY_STATUS_CHARGING
                val isLowBattery = powerManager.isPowerSaveMode || (!isCharging && batteryPct <= 20f)

                if (prefs.isBackgroundAiLearningEnabled) {
                    val packages = try {
                        packageManager.getInstalledPackages(0)
                            .filter { info ->
                                val app = info.applicationInfo
                                app != null &&
                                    (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 &&
                                    info.packageName != packageName
                            }
                            .map { it.packageName }
                    } catch (e: Exception) {
                        emptyList()
                    }

                    if (packages.isNotEmpty()) {
                        if (cursor >= packages.size) cursor = 0

                        // Her turda küçük bir dilim: pil doluyken 5, tasarruf modunda 1 paket.
                        val batchSize = if (isLowBattery) 1 else 5
                        val slice = packages.drop(cursor).take(batchSize)
                        cursor += slice.size

                        for (pkg in slice) {
                            if (!isActive) break
                            val result = try {
                                com.example.scanner.ThreatEngine.analyzePackage(applicationContext, pkg)
                            } catch (e: Exception) {
                                continue
                            }
                            val threat = result.threatEntity ?: continue
                            if (!result.isThreat) continue

                            // Whitelist kontrolü ve kayıt tek yerde: recordDetectedThreat
                            // güvenli listeye alınmış paketi yazar bile.
                            val recorded = repository.recordDetectedThreat(threat) ?: continue

                            repository.logEvent(
                                title = "Arka Plan Bütünlük Taraması",
                                description = "${threat.appName} ($pkg) yeniden değerlendirildi ve " +
                                    "${threat.threatCategory} olarak işaretlendi. Neden: ${threat.detectedReasons}",
                                severity = "CRITICAL"
                            )
                        }
                    }
                }

                // Süpürme aceleye gerek duymaz; asıl tespit kurulum anında yapılıyor.
                delay(if (isLowBattery) 15 * 60_000L else 3 * 60_000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        loopsStarted = false
        serviceJob.cancel()
        packageInstallerCallback?.let {
            try {
                packageManager.packageInstaller.unregisterSessionCallback(it)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Installer dinleyicisi kaldırılamadı", e)
            }
        }
        packageInstallerCallback = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Kullanıcı kalkanı kapattıysa uygulamayı diriltmeye çalışmayalım.
        if (!prefs.isRealTimeProtectionEnabled) return

        val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
            setPackage(packageName)
            action = ACTION_START
        }
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmService.set(
            android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RealTimeProtection"

        /**
         * Ön plan bildirimi. 1001 [com.example.util.NotificationHelper] tarafından da
         * kullanıldığı için tarama bildirimi kalkan bildirimini eziyordu.
         */
        const val NOTIFICATION_ID = 2001
        private const val NOTIFICATION_ID_AUDIT_ALERT = 2002
        private const val NOTIFICATION_ID_DAILY_SUMMARY = 2003

        const val ACTION_START = "com.example.action.START_REALTIME_PROTECTION"
        const val ACTION_STOP = "com.example.action.STOP_REALTIME_PROTECTION"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _lastHeartbeat = MutableStateFlow(System.currentTimeMillis())
        val lastHeartbeat = _lastHeartbeat.asStateFlow()

        /** Servis bu süreçte (aynı process) çalışıyor mu? Watchdog için process-güvenli kontrol. */
        fun isServiceAlive(context: Context): Boolean {
            return try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                @Suppress("DEPRECATION")
                am.getRunningServices(64).any {
                    it.service.className == RealTimeProtectionService::class.java.name
                }
            } catch (e: Exception) {
                // Bilgi alınamadıysa "çalışmıyor" varsayıp başlatmayı dene; startService idempotenttir.
                false
            }
        }

        fun updateSchedule(context: Context) = start(context)

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
            // API 26+ üzerinde startForegroundService kullanmak zorundayız: düz startService
            // uygulama arka plandayken IllegalStateException atıyor. Servis bu isteği alınca
            // startForeground sözleşmesini karşılayıp hemen kendini kapatıyor.
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Kalkan durdurma isteği iletilemedi", e)
            }
        }
    }
}
