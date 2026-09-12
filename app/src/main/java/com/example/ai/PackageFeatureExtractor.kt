package com.example.ai

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.example.data.intel.ThreatIntel
import com.example.data.intel.ThreatIntelStore
import com.example.scanner.InstallSource
import com.example.scanner.SignatureInfo
import com.example.scanner.SignatureVerifier

/**
 * Bir paketi, modelin okuyabileceği sabit uzunlukta sayısal vektöre çevirir.
 *
 * ## Bu dosya neden var
 *
 * Projede 50 girdili bir TFLite modeli (`singularity.tflite`) yüklüydü ama modeli
 * besleyen bir özellik çıkarıcı hiç yoktu; tek çağrı `FloatArray(50) { Random.nextFloat() }`
 * üreten sahte eğitim döngüsüydü. Yani modelin 50 girdisinin ne anlama geldiği
 * hiçbir yerde yazılı değildi. Model olmadan özellik şeması, şema olmadan da
 * anlamlı çıkarım mümkün değil — bu dosya o sözleşmeyi tanımlar.
 *
 * ## Sözleşme
 *
 * Vektör **sıra bağımlıdır**: [SPEC_VERSION] değişmeden indeks anlamları değişemez.
 * Bir özellik eklemek/çıkarmak ya da yeniden sıralamak [SPEC_VERSION] artırmayı
 * ve modelin yeniden eğitilmesini gerektirir. Model, adında `-fsN` soneki taşıyarak
 * hangi şemaya göre eğitildiğini bildirir (ör. `singularity-fs1.tflite`);
 * [AaeSecurityEngine.isInferenceTrustworthy] bunu doğrular.
 *
 * Tüm değerler 0.0–1.0 aralığına normalize edilir; ikili özellikler 0.0 veya 1.0'dır.
 */
object PackageFeatureExtractor {

    /** Özellik şeması sürümü. Sıra veya anlam değişirse artır. */
    const val SPEC_VERSION = 1

    /** Vektör uzunluğu. Gömülü modelin girdi boyutuyla (50) uyumlu tutuldu. */
    const val FEATURE_COUNT = 50

    /**
     * 0–23: izin bayrakları. Sıra sabittir; sona ekleme yapılabilir,
     * araya ekleme [SPEC_VERSION] artırır.
     */
    private val PERMISSION_FEATURES = listOf(
        "android.permission.INTERNET",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_CALL_LOG",
        "android.permission.CALL_PHONE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.WAKE_LOCK",
        "android.permission.DISABLE_KEYGUARD"
    )

    /** Şemanın insan tarafından okunabilir hâli; yeniden eğitim betiği bunu kullanır. */
    val FEATURE_NAMES: List<String> = buildList {
        PERMISSION_FEATURES.forEach { add("perm:${it.substringAfterLast('.')}") }   // 0-23
        add("src:play")                 // 24
        add("src:recognized_store")     // 25
        add("src:manual_sideload")      // 26
        add("src:unknown_installer")    // 27
        add("sig:debug_certificate")    // 28
        add("sig:multiple_signers")     // 29
        add("sig:has_rotation_history") // 30
        add("sig:unreadable")           // 31
        add("id:brand_claim")           // 32
        add("id:brand_cert_known")      // 33
        add("id:brand_cert_mismatch")   // 34
        add("intel:package_match")      // 35
        add("intel:cert_match")         // 36
        add("app:system")               // 37
        add("app:updated_system")       // 38
        add("app:debuggable")           // 39
        add("app:target_sdk")           // 40
        add("app:min_sdk")              // 41
        add("count:permissions")        // 42
        add("count:activities")         // 43
        add("count:services")           // 44
        add("count:receivers")          // 45
        add("count:providers")          // 46
        add("name:suspicious_token")    // 47
        add("meta:version_code")        // 48
        add("meta:install_age_days")    // 49
    }

    init {
        require(FEATURE_NAMES.size == FEATURE_COUNT) {
            "Özellik şeması tutarsız: ${FEATURE_NAMES.size} ad, $FEATURE_COUNT bekleniyor"
        }
    }

    private val SUSPICIOUS_NAME_TOKENS = listOf("spy", "stealer", "trojan", "malware", "keylog", "adware", "crack", "mod")

    /**
     * Paketten [FEATURE_COUNT] uzunluğunda özellik vektörü üretir.
     * Okunamayan alanlar 0.0 kalır; hiçbir değer uydurulmaz.
     */
    fun extract(context: Context, packageInfo: PackageInfo): FloatArray =
        extract(
            context = context,
            packageInfo = packageInfo,
            signature = SignatureVerifier.inspect(context, packageInfo.packageName),
            installSource = SignatureVerifier.classifyInstallSource(context, packageInfo.packageName),
            intel = ThreatIntelStore.current(context)
        )

    /**
     * Tarama motoru için hızlı yol. [ThreatEngine] bu değerleri tespit sırasında
     * zaten hesaplıyor; sertifika ayrıştırma işlemi pahalı olduğundan (tam taramada
     * yüzlerce paket × her biri iki kez) yeniden hesaplamak yerine eldekini verir.
     */
    fun extract(
        context: Context,
        packageInfo: PackageInfo,
        signature: SignatureInfo?,
        installSource: InstallSource,
        intel: ThreatIntel
    ): FloatArray {
        val features = FloatArray(FEATURE_COUNT)
        val pkgName = packageInfo.packageName
        val appInfo = packageInfo.applicationInfo
        val permissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()

        // 0-23: izinler
        PERMISSION_FEATURES.forEachIndexed { index, permission ->
            features[index] = if (permission in permissions) 1f else 0f
        }

        // 24-27: kurulum kaynağı (one-hot)
        when (installSource) {
            InstallSource.PLAY_STORE -> features[24] = 1f
            InstallSource.RECOGNIZED_STORE -> features[25] = 1f
            InstallSource.MANUAL_SIDELOAD -> features[26] = 1f
            InstallSource.UNKNOWN_INSTALLER -> features[27] = 1f
            InstallSource.PREINSTALLED -> Unit
        }

        // 28-36: imza ve kimlik
        if (signature == null) {
            features[31] = 1f
        } else {
            features[28] = if (signature.isDebugCertificate) 1f else 0f
            features[29] = if (signature.hasMultipleSigners) 1f else 0f
            features[30] = if (signature.historyDigests.size > 1) 1f else 0f
            if (signature.currentDigests.any { it in intel.maliciousCertificates }) features[36] = 1f
        }

        features[32] = if (intel.brandClaimFor(pkgName) != null) 1f else 0f
        val brandDigests = intel.brandCertificates[pkgName]
        if (brandDigests != null) {
            features[33] = 1f
            features[34] = if (signature != null && signature.currentDigests.none { it in brandDigests }) 1f else 0f
        }
        features[35] = if (pkgName in intel.maliciousPackages) 1f else 0f

        // 37-41: uygulama üst verisi
        if (appInfo != null) {
            features[37] = if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) 1f else 0f
            features[38] = if ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) 1f else 0f
            features[39] = if ((appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) 1f else 0f
            features[40] = normalize(appInfo.targetSdkVersion.toFloat(), 36f)
            features[41] = normalize(appInfo.minSdkVersion.toFloat(), 36f)
        }

        // 42-46: bileşen sayıları (yoğunluk göstergesi olarak normalize)
        features[42] = normalize(permissions.size.toFloat(), 80f)
        features[43] = normalize(packageInfo.activities?.size?.toFloat() ?: 0f, 100f)
        features[44] = normalize(packageInfo.services?.size?.toFloat() ?: 0f, 40f)
        features[45] = normalize(packageInfo.receivers?.size?.toFloat() ?: 0f, 40f)
        features[46] = normalize(packageInfo.providers?.size?.toFloat() ?: 0f, 20f)

        // 47-49: ad ve yaş
        val lower = "$pkgName ${appInfo?.let { runCatching { context.packageManager.getApplicationLabel(it).toString() }.getOrNull() } ?: ""}".lowercase()
        features[47] = if (SUSPICIOUS_NAME_TOKENS.any { lower.contains(it) }) 1f else 0f
        features[48] = normalize(packageInfo.longVersionCodeCompat().toFloat(), 1_000_000f)
        val ageDays = (System.currentTimeMillis() - packageInfo.firstInstallTime) / 86_400_000f
        features[49] = normalize(ageDays, 730f)

        return features
    }

    private fun normalize(value: Float, max: Float): Float = (value / max).coerceIn(0f, 1f)

    private fun PackageInfo.longVersionCodeCompat(): Long =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) longVersionCode
        else @Suppress("DEPRECATION") versionCode.toLong()
}
