package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.AntivirusApplication
import com.example.MainActivity
import com.example.ui.ThreatAlertActivity
import com.example.R
import com.example.scanner.ThreatEngine
import com.example.util.ProtectionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) {
            return
        }

        val uri = intent.data ?: return
        val packageName = uri.schemeSpecificPart ?: return

        // Skip our own package
        if (packageName == context.packageName) return

        val prefs = ProtectionPreferences(context)
        if (!prefs.isAutoScanNewAppsEnabled) return

        val appContext = context.applicationContext as AntivirusApplication
        val repository = appContext.repository

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
            val result = ThreatEngine.analyzePackage(context, packageName)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "THREATS")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                packageName.hashCode(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (result.isThreat && result.threatEntity != null) {
                val threat = result.threatEntity
                repository.insertThreat(threat)
                repository.logEvent(
                    title = "Gerçek Zamanlı Tehdit Tespit Edildi!",
                    description = "${threat.appName} (${threat.packageName}) cihazınıza yüklendi ve ${threat.threatCategory} olarak sınıflandırıldı.",
                    severity = "CRITICAL"
                )

                val alertIntent = Intent(context, ThreatAlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("APP_NAME", threat.appName)
                    putExtra("PKG_NAME", threat.packageName)
                    putExtra("CATEGORY", threat.threatCategory)
                    putExtra("REASONS", threat.detectedReasons)
                }
                context.startActivity(alertIntent)
                
                val alertNotification = NotificationCompat.Builder(
                    context,
                    AntivirusApplication.CHANNEL_THREAT_ALERTS_ID
                )
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle("GÜVENLİK TEHDİDİ ENGELLENDİ")
                    .setContentText("${threat.appName} şüpheli izinler içeriyor! Hemen inceleyin.")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("${threat.appName} yüklendi.\nRisk Seviyesi: ${threat.riskLevel}\nKategori: ${threat.threatCategory}\nNedenler: ${threat.detectedReasons}")
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(packageName.hashCode(), alertNotification)
            } else {
                val appLabel = try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }

                repository.logEvent(
                    title = "Yeni Uygulama Doğrulandı",
                    description = "$appLabel ($packageName) gerçek zamanlı tarandı. Herhangi bir tehdide rastlanmadı.",
                    severity = "SUCCESS"
                )

                val safeNotification = NotificationCompat.Builder(
                    context,
                    AntivirusApplication.CHANNEL_REALTIME_ID
                )
                    .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                    .setContentTitle("Uygulama Tarandı: Güvenli")
                    .setContentText("$appLabel temiz ve güvenli bulundu.")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(packageName.hashCode(), safeNotification)
            }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
