package com.example.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** Uygulamanın APK'sını imzalayan sertifikalardan çıkarılan gerçek veriler. */
data class SignatureInfo(
    /** APK'yı şu anda imzalayan sertifikaların SHA-256 özetleri (büyük harf hex). */
    val currentDigests: List<String>,
    /** İmza rotasyonu geçmişindeki tüm sertifikaların özetleri (API 28+). */
    val historyDigests: List<String>,
    /** Sertifika Android'in varsayılan hata ayıklama anahtarıyla mı üretilmiş? */
    val isDebugCertificate: Boolean,
    val hasMultipleSigners: Boolean,
    val subjectDn: String?
) {
    val primaryDigest: String? get() = currentDigests.firstOrNull()
    val shortDigest: String get() = primaryDigest?.take(16) ?: "—"
}

enum class SignaturePinResult {
    /** Bu paketi ilk kez görüyoruz; mevcut imza referans olarak kaydedildi. */
    FIRST_SEEN,

    /** İmza daha önce kaydettiğimizle birebir aynı. */
    MATCHES,

    /** İmza değişti ama yeni sertifika, uygulamanın kendi rotasyon geçmişinde var (meşru). */
    ROTATED,

    /** İmza değişti ve rotasyon geçmişinde yok: uygulama farklı bir anahtarla yeniden paketlenmiş. */
    CHANGED,

    /** İmza okunamadı. */
    UNKNOWN
}

data class PinVerdict(
    val result: SignaturePinResult,
    val previousDigest: String? = null
)

enum class InstallSource {
    /** Google Play — kriptografik doğrulamadan geçmiş. */
    PLAY_STORE,

    /** F-Droid, Aurora, Galaxy Store, AppGallery gibi tanınan alternatif mağazalar. */
    RECOGNIZED_STORE,

    /** Kullanıcının elle kurduğu APK (paket yükleyici / adb). */
    MANUAL_SIDELOAD,

    /** Başka bir uygulama tarafından kurulmuş — dropper vektörü olabilir. */
    UNKNOWN_INSTALLER,

    /** Cihazla birlikte gelen, yükleyicisi olmayan paket. */
    PREINSTALLED
}

/**
 * Paket imzalarını gerçekten okuyup doğrulayan bileşen.
 *
 * Eskiden [ThreatEngine] içinde "Sub-AI SHA-256 sertifikasını analiz eder" denip
 * aslında yalnızca uygulama adında "whatsapp" geçip geçmediğine bakılıyordu.
 * Burada imzalar `GET_SIGNING_CERTIFICATES` ile okunup gerçekten özetleniyor.
 *
 * Klon tespiti "sabitleme (pinning)" ile yapılır: bir paketi ilk gördüğümüzde
 * imzasını kaydederiz; imza sonradan değişir ve yeni sertifika uygulamanın kendi
 * rotasyon geçmişinde yoksa, paket farklı bir anahtarla yeniden paketlenmiş demektir.
 * Android meşru güncellemelerde imza değişimine izin vermediği için bu güçlü bir sinyaldir.
 */
object SignatureVerifier {

    private const val PIN_PREFS = "antivirus_signature_pins"
    private const val DEBUG_CERT_MARKER = "CN=Android Debug"

    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    private val RECOGNIZED_STORES = setOf(
        "org.fdroid.fdroid",
        "org.fdroid.basic",
        "com.aurora.store",
        "com.amazon.venezia",
        "com.amazon.mShop.android.shopping",
        "com.sec.android.app.samsungapps",
        "com.huawei.appmarket",
        "com.xiaomi.market",
        "com.xiaomi.mipicks",
        "com.oppo.market",
        "com.heytap.market",
        "com.bbk.appstore",
        "com.farsitel.bazaar"
    )

    private val MANUAL_INSTALLERS = setOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.android.shell"
    )

    // ---------------------------------------------------------------- imzalar

    fun inspect(context: Context, packageName: String): SignatureInfo? {
        return try {
            val pm = context.packageManager
            val current = readCurrentSigners(pm, packageName) ?: return null
            if (current.isEmpty()) return null

            val history = readSigningHistory(pm, packageName)
            val currentDigests = current.map { sha256Hex(it.toByteArray()) }
            val historyDigests = history.map { sha256Hex(it.toByteArray()) }

            var isDebug = false
            var subject: String? = null
            for (signature in current) {
                val dn = readSubjectDn(signature) ?: continue
                if (subject == null) subject = dn
                if (dn.contains(DEBUG_CERT_MARKER, ignoreCase = true)) isDebug = true
            }

            SignatureInfo(
                currentDigests = currentDigests,
                historyDigests = historyDigests,
                isDebugCertificate = isDebug,
                hasMultipleSigners = current.size > 1,
                subjectDn = subject
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun readCurrentSigners(pm: PackageManager, packageName: String): List<Signature>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = getPackageInfoWithFlags(pm, packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo ?: return null
            signingInfo.apkContentsSigners?.toList()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures?.filterNotNull()?.toList()
        }
    }

    private fun readSigningHistory(pm: PackageManager, packageName: String): List<Signature> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptyList()
        return try {
            val info = getPackageInfoWithFlags(pm, packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo ?: return emptyList()
            if (signingInfo.hasMultipleSigners()) {
                emptyList()
            } else {
                signingInfo.signingCertificateHistory?.toList() ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getPackageInfoWithFlags(pm: PackageManager, packageName: String, flags: Int) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }

    private fun readSubjectDn(signature: Signature): String? = try {
        val factory = CertificateFactory.getInstance("X.509")
        val certificate = factory.generateCertificate(signature.toByteArray().inputStream()) as X509Certificate
        certificate.subjectDN.name
    } catch (e: Exception) {
        null
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val builder = StringBuilder(digest.size * 2)
        for (byte in digest) {
            builder.append("0123456789ABCDEF"[(byte.toInt() shr 4) and 0x0F])
            builder.append("0123456789ABCDEF"[byte.toInt() and 0x0F])
        }
        return builder.toString()
    }

    // ------------------------------------------------------------ sabitleme

    fun checkPin(context: Context, packageName: String, info: SignatureInfo): PinVerdict {
        val current = info.primaryDigest ?: return PinVerdict(SignaturePinResult.UNKNOWN)
        val prefs = context.getSharedPreferences(PIN_PREFS, Context.MODE_PRIVATE)
        val pinned = prefs.getString(packageName, null)

        if (pinned == null) {
            prefs.edit().putString(packageName, current).apply()
            return PinVerdict(SignaturePinResult.FIRST_SEEN)
        }
        if (pinned == current) return PinVerdict(SignaturePinResult.MATCHES)

        // Meşru anahtar rotasyonu: yeni sertifika uygulamanın kendi geçmişinde.
        if (info.historyDigests.contains(pinned) || info.currentDigests.contains(pinned)) {
            prefs.edit().putString(packageName, current).apply()
            return PinVerdict(SignaturePinResult.ROTATED, previousDigest = pinned)
        }

        // Gerçek değişiklik. Aynı uyarının her taramada tekrarlanmaması için
        // referansı güncelliyoruz; tespit zaten tehdit kaydına yazılıyor.
        prefs.edit().putString(packageName, current).apply()
        return PinVerdict(SignaturePinResult.CHANGED, previousDigest = pinned)
    }

    fun forgetPin(context: Context, packageName: String) {
        context.getSharedPreferences(PIN_PREFS, Context.MODE_PRIVATE)
            .edit().remove(packageName).apply()
    }

    // -------------------------------------------------------- dağıtım kanalı

    fun classifyInstallSource(context: Context, packageName: String): InstallSource {
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }

        return when {
            installer == PLAY_STORE_PACKAGE -> InstallSource.PLAY_STORE
            installer in RECOGNIZED_STORES -> InstallSource.RECOGNIZED_STORE
            installer in MANUAL_INSTALLERS -> InstallSource.MANUAL_SIDELOAD
            installer == null -> InstallSource.MANUAL_SIDELOAD
            installer == context.packageName -> InstallSource.MANUAL_SIDELOAD
            else -> InstallSource.UNKNOWN_INSTALLER
        }
    }

    fun describeInstallSource(source: InstallSource): String = when (source) {
        InstallSource.PLAY_STORE -> "Google Play"
        InstallSource.RECOGNIZED_STORE -> "tanınan alternatif mağaza"
        InstallSource.MANUAL_SIDELOAD -> "elle kurulan APK"
        InstallSource.UNKNOWN_INSTALLER -> "başka bir uygulama tarafından kurulmuş"
        InstallSource.PREINSTALLED -> "cihazla birlikte gelen paket"
    }
}
