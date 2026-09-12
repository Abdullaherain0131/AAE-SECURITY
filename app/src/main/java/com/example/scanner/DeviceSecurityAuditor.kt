package com.example.scanner

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.io.File

data class SecurityAuditItem(
    val title: String,
    val description: String,
    val isSecure: Boolean,
    val severity: String, // "SECURE", "WARNING", "CRITICAL"
    val recommendation: String,
    val settingsAction: String? = null
)

data class DeviceSecurityReport(
    val healthScore: Int, // 0 - 100
    val totalChecks: Int,
    val passedChecks: Int,
    val auditItems: List<SecurityAuditItem>
)

object DeviceSecurityAuditor {

    fun performAudit(context: Context): DeviceSecurityReport {
        val items = mutableListOf<SecurityAuditItem>()

        // 1. Root / Superuser detection
        val isRooted = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
        items.add(
            SecurityAuditItem(
                title = "Root & Cihaz Bütünlüğü",
                description = if (isRooted) "Cihazda yetkisiz kök erişimi (Root) veya test anahtarları tespit edildi!" else "Sistem bütünlüğü koruma altında, root izine rastlanmadı.",
                isSecure = !isRooted,
                severity = if (isRooted) "CRITICAL" else "SECURE",
                recommendation = if (isRooted) "Rootlu cihazlar bankacılık ve veri hırsızlığına karşı savunmasızdır." else "Cihaz resmi Android güvenlik standartlarına uygun."
            )
        )

        // 2. Screen Lock (Keyguard) Security
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLockSecure = km?.isDeviceSecure ?: false
        items.add(
            SecurityAuditItem(
                title = "Ekran Kilidi Koruması",
                description = if (isLockSecure) "PIN, Desen veya Biyometrik ekran kilidi aktif." else "Cihazınızda ekran kilidi ayarlanmamış! Fiziksel erişim riski yüksek.",
                isSecure = isLockSecure,
                severity = if (isLockSecure) "SECURE" else "WARNING",
                recommendation = if (isLockSecure) "Cihaz kilit koruması aktif." else "Ayarlardan güçlü bir PIN veya parmak izi kilidi tanımlayın.",
                settingsAction = Settings.ACTION_SECURITY_SETTINGS
            )
        )

        // 3. USB Debugging & Developer Options
        val devOptionsEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
        val adbEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
        val devRisk = devOptionsEnabled || adbEnabled
        items.add(
            SecurityAuditItem(
                title = "USB Hata Ayıklama & Geliştirici Modu",
                description = if (devRisk) "Geliştirici seçenekleri veya USB hata ayıklama açık. Kablo ile yetkisiz veri aktarımı riski mevcut." else "Geliştirici seçenekleri kapalı, portlar güvende.",
                isSecure = !devRisk,
                severity = if (devRisk) "WARNING" else "SECURE",
                recommendation = if (devRisk) "Kullanmadığınız durumlarda Geliştirici Seçeneklerini kapatmanız önerilir." else "Harici bağlantı güvenliği tam.",
                settingsAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            )
        )

        // 4. Android Version & Security Patch
        val patchDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            "Bilinmiyor"
        }
        items.add(
            SecurityAuditItem(
                title = "Android Güvenlik Yaması",
                description = "Mevcut yama sürümü: $patchDate (Android ${Build.VERSION.RELEASE})",
                isSecure = true,
                severity = "SECURE",
                recommendation = "Cihazınızı her zaman en güncel sistem yazılımında tutun."
            )
        )

        // 5. Unknown Sources / Third-Party Install setting
        val unknownSourcesEnabled = try {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        } catch (e: Exception) {
            false
        }
        items.add(
            SecurityAuditItem(
                title = "Bilinmeyen Kaynaklardan Yükleme",
                description = if (unknownSourcesEnabled) "Mağaza dışı kaynaklardan serbest APK yükleme açık." else "Bilinmeyen kaynaklar kısıtlı, güvenli koruma etkin.",
                isSecure = !unknownSourcesEnabled,
                severity = if (unknownSourcesEnabled) "WARNING" else "SECURE",
                recommendation = if (unknownSourcesEnabled) "Yalnızca resmi mağazalardan uygulama yükleyin." else "Kötü amaçlı harici APK yüklemeleri engelleniyor."
            )
        )

        val passed = items.count { it.isSecure }
        val total = items.size
        val score = ((passed.toDouble() / total.toDouble()) * 100).toInt()

        return DeviceSecurityReport(
            healthScore = score,
            totalChecks = total,
            passedChecks = passed,
            auditItems = items
        )
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = process.inputStream.bufferedReader()
            reader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }
}
