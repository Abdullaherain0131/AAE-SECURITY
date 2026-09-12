package com.example.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.AntivirusApplication
import com.example.MainActivity
import com.example.scanner.ThreatEngine
import com.example.ui.ThreatAlertActivity

/**
 * Aynı paketin kısa süre içinde birden fazla kez analiz edilmesini engeller.
 *
 * Yeni bir kurulum iki ayrı yoldan haber verilebiliyor:
 *  - [com.example.receiver.PackageInstallReceiver] → ACTION_PACKAGE_ADDED / REPLACED yayını
 *  - [com.example.service.RealTimeProtectionService] → PackageInstaller.SessionCallback
 *
 * Her ikisi de aynı işlemi yaptığı için kullanıcı iki bildirim + iki uyarı ekranı görüyordu.
 * Hangi yol önce gelirse işi o yapar, diğeri sessizce düşer.
 */
object RecentAnalysisGuard {
    private const val DEFAULT_WINDOW_MS = 60_000L
    private val handled = HashMap<String, Long>()

    @Synchronized
    fun shouldHandle(packageName: String, windowMs: Long = DEFAULT_WINDOW_MS): Boolean {
        val now = System.currentTimeMillis()

        val iterator = handled.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value > windowMs) iterator.remove()
        }

        val last = handled[packageName]
        if (last != null && now - last <= windowMs) return false
        handled[packageName] = now
        return true
    }

    @Synchronized
    fun forget(packageName: String) {
        handled.remove(packageName)
    }
}

/**
 * Yeni kurulan bir uygulamayı analiz eden, kaydeden ve kullanıcıyı bilgilendiren tek merkez.
 * Gerçek zamanlı korumanın iki giriş noktası da buraya bağlanır.
 */
object NewAppInspector {

    const val SOURCE_BROADCAST = "Yayın Alıcısı"
    const val SOURCE_INSTALLER_SESSION = "Installer Listener"

    /**
     * @return analiz gerçekten yapıldıysa true, atlandıysa (kapalı ayar / mükerrer olay) false.
     */
    suspend fun inspectInstalledPackage(
        context: Context,
        packageName: String,
        source: String
    ): Boolean {
        val appContext = context.applicationContext
        if (packageName == appContext.packageName) return false

        val prefs = ProtectionPreferences(appContext)
        if (!prefs.isAutoScanNewAppsEnabled) return false

        // Mükerrer olayları burada kes; iki giriş noktası da aynı kurulumu görebiliyor.
        if (!RecentAnalysisGuard.shouldHandle(packageName)) return false

        val repository = (appContext as AntivirusApplication).repository
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kullanıcı bu paketi daha önce güvenli listeye aldıysa tekrar rahatsız etmeyelim.
        val existing = repository.findThreatByPackage(packageName)
        if (existing != null && existing.status == "GÜVENLİ_LİSTE") {
            repository.logEvent(
                title = "Güncelleme Doğrulandı",
                description = "${existing.appName} güncellendi. Uygulama güvenli listenizde olduğu için uyarı gösterilmedi.",
                severity = "INFO"
            )
            return true
        }

        val result = ThreatEngine.analyzePackage(appContext, packageName)
        val threat = result.threatEntity

        if (result.isThreat && threat != null) {
            // recordDetectedThreat whitelist'e saygı duyar ve satır çoğalmasını önler;
            // kurulum anı tespitinde null dönmesi yalnızca güvenli liste kararından olur,
            // o durumda yukarıda zaten ele alındı ve buraya gelinmez.
            repository.recordDetectedThreat(threat)
            repository.logEvent(
                title = "Gerçek Zamanlı Tehdit Tespit Edildi!",
                description = "${threat.appName} (${threat.packageName}) cihazınıza yüklendi ve " +
                    "${threat.threatCategory} olarak sınıflandırıldı. [$source]",
                severity = "CRITICAL"
            )

            notificationManager.notify(
                packageName.hashCode(),
                buildThreatNotification(appContext, threat.appName, threat.packageName, threat.riskLevel, threat.threatCategory, threat.detectedReasons)
            )

            // Arka plandan Activity açmak SYSTEM_ALERT_WINDOW izni olmadan engellenebilir;
            // bildirim yukarıda zaten gönderildiği için burada hata yutulabilir.
            try {
                val alertIntent = Intent(appContext, ThreatAlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("APP_NAME", threat.appName)
                    putExtra("PKG_NAME", threat.packageName)
                    putExtra("CATEGORY", threat.threatCategory)
                    putExtra("REASONS", threat.detectedReasons)
                }
                appContext.startActivity(alertIntent)
            } catch (e: Exception) {
                android.util.Log.w("NewAppInspector", "Uyarı ekranı açılamadı, bildirim gönderildi", e)
            }
            return true
        }

        val appLabel = resolveAppLabel(appContext, packageName)
        repository.logEvent(
            title = "Yeni Uygulama Doğrulandı",
            description = "$appLabel ($packageName) gerçek zamanlı tarandı. Herhangi bir tehdide rastlanmadı. [$source]",
            severity = "SUCCESS"
        )
        notificationManager.notify(
            packageName.hashCode(),
            buildSafeNotification(appContext, appLabel)
        )
        return true
    }

    private fun resolveAppLabel(context: Context, packageName: String): String = try {
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }

    private fun openAppPendingIntent(context: Context, packageName: String): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "THREATS")
        }
        return PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildThreatNotification(
        context: Context,
        appName: String,
        packageName: String,
        riskLevel: String,
        category: String,
        reasons: String
    ) = NotificationCompat.Builder(context, AntivirusApplication.CHANNEL_THREAT_ALERTS_ID)
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setContentTitle("GÜVENLİK TEHDİDİ TESPİT EDİLDİ")
        .setContentText("$appName şüpheli davranış işaretleri taşıyor! Hemen inceleyin.")
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                "$appName yüklendi.\nRisk Seviyesi: $riskLevel\nKategori: $category\nNedenler: $reasons"
            )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(openAppPendingIntent(context, packageName))
        .build()

    private fun buildSafeNotification(context: Context, appLabel: String) =
        NotificationCompat.Builder(context, AntivirusApplication.CHANNEL_REALTIME_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Uygulama Tarandı: Güvenli")
            .setContentText("$appLabel temiz ve güvenli bulundu.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
}
