package com.example.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.ai.AaeEnsembleScorer
import com.example.ai.AaeSecurityEngine
import com.example.ai.PackageFeatureExtractor
import com.example.data.entity.ThreatEntity
import com.example.data.intel.ThreatIntel
import com.example.data.intel.ThreatIntelStore

data class ThreatAnalysisResult(
    val isThreat: Boolean,
    val threatEntity: ThreatEntity? = null
)

/**
 * Paket tespit çekirdeği. Elle tarama, zamanlanmış tarama, periyodik tarama ve
 * gerçek zamanlı koruma — dördü de buraya bağlanır.
 *
 * ## Güven modeli
 *
 * Motor kimliği **imzadan** türetir, paket adından değil. Bu ayrım kritik:
 * paket adı ucuzdur, herkes APK'sını `com.whatsapp.pro` diye paketleyebilir;
 * imzalama anahtarı ise taklit edilemez.
 *
 * Önceki sürüm bunun tersini yapıyordu: paket adı tanıdık bir ön ekle
 * (`com.whatsapp`, `com.garanti` …) başlıyorsa hiçbir kontrol yapmadan "temiz"
 * diyordu. Yani klon tespitinin var olma sebebi olan senaryo — sahte WhatsApp,
 * sahte banka uygulaması — geçmesi **garanti** olan tek senaryoydu. Aynı blokta
 * "[Sub-AI Analizi] Çalıntı İmza Tespit Edildi" diyen kod ise hiçbir sertifika
 * okumuyor, yalnızca uygulama adında "bank"/"whatsapp" geçip geçmediğine bakıyordu.
 *
 * Artık o ön ek listesi ([ThreatIntel.KNOWN_BRAND_PREFIXES]) beyaz liste değil,
 * **doğrulama yükümlülüğü** tetikleyicisidir: markanın adını taşıyan paket,
 * o markaya ait olduğunu imzasıyla ispatlamak zorundadır.
 *
 * ## Sinyal katmanları
 *
 * 1. **Kimlik** — [SignatureVerifier] ile gerçek sertifika özeti, imza sabitleme
 *    (pinning) ve dağıtım kanalı. En ağır puanlar burada.
 * 2. **İstihbarat** — [ThreatIntelStore]'daki bilinen zararlı paket/sertifika kümesi.
 *    Çevrimdışı çalışır; Wi-Fi varken tazelenir.
 * 3. **Topluluk skoru** — [AaeEnsembleScorer], [PackageFeatureExtractor] vektörü
 *    üzerinden izin desenleri, dağıtım kanalı ve yapı anomalilerini tek tek
 *    adlandırılmış katkılar hâlinde puanlar. Model `isInferenceTrustworthy`
 *    olduğunda model skoruyla uzlaştırılır. Kimlik katmanı paketi doğrulamışsa bu
 *    katman bastırılır, çünkü meşru uygulamalarda geniş izin normaldir.
 */
object ThreatEngine {

    private val DANGEROUS_NAME_KEYWORDS = listOf(
        "spy", "stealer", "trojan", "malware", "keylog", "adware"
    )

    /** Tespit eşiği. Bunun altındaki paketler tehdit olarak raporlanmaz. */
    private const val THREAT_THRESHOLD = 40

    // ------------------------------------------------------------------ giriş noktaları

    fun analyzePackage(context: Context, packageName: String): ThreatAnalysisResult {
        val pm = context.packageManager
        return try {
            // Bileşen sayıları (etkinlik/servis/alıcı/sağlayıcı) topluluk skorunun
            // girdisidir; yalnızca GET_PERMISSIONS ile sorgulanırsa hepsi boş gelir.
            val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS
            val packageInfo = getPackageInfo(pm, packageName, flags)
            evaluatePackageInfo(context, packageInfo)
        } catch (e: Exception) {
            ThreatAnalysisResult(isThreat = false)
        }
    }

    private fun getPackageInfo(pm: PackageManager, packageName: String, flags: Int): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }

    fun evaluatePackageInfo(context: Context, packageInfo: PackageInfo): ThreatAnalysisResult {
        val pm = context.packageManager
        val appInfo = packageInfo.applicationInfo ?: return ThreatAnalysisResult(isThreat = false)
        val pkgName = packageInfo.packageName

        // Kendimizi işaretlemeyelim.
        if (pkgName == context.packageName) return ThreatAnalysisResult(isThreat = false)

        // Güncellenmemiş sistem uygulamaları platformun parçasıdır.
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (isSystemApp && !isUpdatedSystemApp) return ThreatAnalysisResult(isThreat = false)

        // Android çatısının kendi paketleri.
        if (pkgName == "android" ||
            pkgName.startsWith("com.android.") ||
            pkgName.startsWith("com.google.android.") ||
            pkgName.startsWith("androidx.")
        ) {
            return ThreatAnalysisResult(isThreat = false)
        }

        val appName = try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkgName
        }

        val intel = ThreatIntelStore.current(context)
        val reasons = mutableListOf<String>()
        var score = 0

        // ================================================== 1. KİMLİK (imza + kanal)

        val signature = SignatureVerifier.inspect(context, pkgName)
        val installSource = SignatureVerifier.classifyInstallSource(context, pkgName)
        val fromTrustedStore = installSource == InstallSource.PLAY_STORE ||
            installSource == InstallSource.RECOGNIZED_STORE

        val brandClaim = intel.brandClaimFor(pkgName)
        val knownBrandDigests = intel.brandCertificates[pkgName]

        // Marka sertifikası elimizde varsa karar kesindir: eşleşiyor ya da eşleşmiyor.
        var identityVerified = false
        if (knownBrandDigests != null && signature != null) {
            if (signature.currentDigests.any { it in knownBrandDigests }) {
                identityVerified = true
            } else {
                reasons.add(
                    "Sahte uygulama: paket \"$pkgName\" adını kullanıyor ancak APK imzası " +
                        "${brandClaim ?: "bu paketin"} gerçek geliştirici sertifikasıyla uyuşmuyor " +
                        "(imza ${signature.shortDigest}…)"
                )
                score += 80
            }
        }

        if (signature == null) {
            reasons.add("Paketin imzalama sertifikası okunamadı (bozuk veya kurcalanmış APK)")
            score += 15
        }

        // Marka adını taşıyan ama kimliği ispatlanamayan paket.
        if (brandClaim != null && !identityVerified) {
            if (!fromTrustedStore) {
                reasons.add(
                    "Marka taklidi riski: paket adı $brandClaim uygulamasına ait görünüyor, " +
                        "fakat ${SignatureVerifier.describeInstallSource(installSource)} yoluyla kurulmuş"
                )
                score += 55
            }
            if (signature?.isDebugCertificate == true) {
                reasons.add("$brandClaim adına imzalanmış paket hata ayıklama (debug) sertifikası taşıyor")
                score += 30
            }
        }

        // İmza sabitleme: paket sonradan farklı bir anahtarla yeniden imzalanmış mı?
        if (signature != null) {
            val pin = SignatureVerifier.checkPin(context, pkgName, signature)
            if (pin.result == SignaturePinResult.CHANGED) {
                reasons.add(
                    "Uygulamanın imzası değişti ve yeni sertifika kendi rotasyon geçmişinde yok: " +
                        "paket farklı bir anahtarla yeniden paketlenmiş (önceki imza " +
                        "${pin.previousDigest?.take(16) ?: "?"}…)"
                )
                score += 70
            }

            if (signature.isDebugCertificate && brandClaim == null && !fromTrustedStore) {
                reasons.add("Uygulama hata ayıklama sertifikasıyla imzalanmış (yayın sürümü değil)")
                score += 20
            }

            val badCert = signature.currentDigests.firstOrNull { it in intel.maliciousCertificates }
            if (badCert != null) {
                reasons.add("İmzalama sertifikası bilinen zararlı yazılım kampanyalarında kullanılmış (${badCert.take(16)}…)")
                score += 100
            }
        }

        // Dağıtım kanalı.
        when (installSource) {
            InstallSource.UNKNOWN_INSTALLER -> {
                reasons.add("Paket başka bir uygulama tarafından kurulmuş (Trojan Dropper vektörü)")
                score += 25
            }
            InstallSource.MANUAL_SIDELOAD -> {
                reasons.add("Play Store dışı kaynaktan (elle kurulan APK) yüklendi")
                score += 10
            }
            else -> Unit
        }

        // ================================================== 2. İSTİHBARAT

        if (pkgName in intel.maliciousPackages) {
            reasons.add("Paket, güncel zararlı yazılım veritabanında doğrudan eşleşti")
            score += 100
        }

        val lowerPkg = pkgName.lowercase()
        val lowerName = appName.lowercase()
        if (DANGEROUS_NAME_KEYWORDS.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            reasons.add("Paket veya uygulama adı bilinen zararlı yazılım terimleri içeriyor")
            score += 30
        }

        // ================================================== 3. TOPLULUK SKORU
        //
        // Özellik vektörü bir kez üretilir; hem topluluk skorlayıcısı hem de (şemaya
        // göre eğitilmişse) TFLite modeli aynı girdiyi görür. Yukarıda hesaplanan
        // imza/kurulum kaynağı/istihbarat yeniden kullanılır — sertifika ayrıştırma
        // pahalıdır, tam taramada ikinci kez yapılmaz.

        val features = PackageFeatureExtractor.extract(
            context = context,
            packageInfo = packageInfo,
            signature = signature,
            installSource = installSource,
            intel = intel
        )
        val ensemble = AaeEnsembleScorer.score(features)

        // Modelin skoru yalnızca özellik şemasıyla eşleşiyorsa (isInferenceTrustworthy)
        // karara girer; aksi hâlde uydurma bir sayıya güvenmek yanlış güvence üretir.
        val ensembleScore: Int
        if (AaeSecurityEngine.isInferenceTrustworthy) {
            val modelScore = AaeSecurityEngine.runInference(features)
                ?.coerceIn(0f, 1f)?.times(100)?.toInt()
            if (modelScore != null) {
                ensembleScore = (ensemble.score + modelScore) / 2
                reasons.add(
                    "Model skoru %$modelScore, topluluk skoru %${ensemble.score} " +
                        "(uzlaştırılmış: %$ensembleScore)"
                )
            } else {
                ensembleScore = ensemble.score
            }
        } else {
            ensembleScore = ensemble.score
        }

        // Kimliği doğrulanmış ya da denetimli mağazadan gelen uygulamalarda geniş izin
        // normaldir (bir SMS uygulaması SMS okur). Bu katmanı yalnızca kimlik zayıfsa
        // uygula; aksi hâlde motorun tamamı yanlış pozitif üretir.
        if (identityVerified || fromTrustedStore) {
            if (ensembleScore > 0) {
                reasons.add(
                    "Not: uygulama geniş izinler istiyor (topluluk skoru %$ensembleScore), " +
                        "ancak kimliği " +
                        (if (identityVerified) "geliştirici sertifikasıyla" else SignatureVerifier.describeInstallSource(installSource)) +
                        " doğrulandığı için tehdit sayılmadı"
                )
            }
        } else {
            reasons.addAll(ensemble.contributions.map { it.label })
            score += ensembleScore
        }

        // ================================================== KARAR

        if (score < THREAT_THRESHOLD) return ThreatAnalysisResult(isThreat = false)

        val isImpersonation = brandClaim != null && !identityVerified

        val riskLevel = when {
            score >= 80 -> "KRİTİK"
            score >= 60 -> "YÜKSEK"
            else -> "ORTA"
        }

        val category = when {
            isImpersonation -> "Sahte / Klon Uygulama"
            score >= 80 && ensemble.smsAbuse -> "Finansal Truva Atı / SMS Stealer"
            ensemble.overlayAbuse -> "Yetkisiz Ekran Katmanı / Phishing"
            ensemble.spyware -> "Casus Yazılım (Spyware)"
            else -> "Şüpheli Zararlı Yazılım"
        }

        val threat = ThreatEntity(
            packageName = pkgName,
            appName = appName,
            versionName = packageInfo.versionName ?: "1.0",
            threatCategory = category,
            riskLevel = riskLevel,
            riskScore = score.coerceIn(THREAT_THRESHOLD, 100),
            detectedReasons = reasons.joinToString(" • "),
            status = "AKTİF"
        )
        return ThreatAnalysisResult(isThreat = true, threatEntity = threat)
    }

    // ------------------------------------------------------------ sistem taraması

    /**
     * Root / özel ROM izlerini arar. Bulgular gerçek dosya sistemi ve paket
     * sorgularına dayanır.
     */
    fun performDeepSystemScan(context: Context): List<ThreatEntity> {
        val threats = mutableListOf<ThreatEntity>()
        var rootScore = 0
        val rootReasons = mutableListOf<String>()

        if (Build.TAGS?.contains("test-keys") == true) {
            rootScore += 30
            rootReasons.add("İşletim sistemi 'test-keys' ile imzalanmış (orijinal fabrika imzası değil)")
        }

        val rootPaths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su", "/magisk/.core/bin/su"
        )
        for (path in rootPaths) {
            if (java.io.File(path).exists()) {
                rootScore += 50
                rootReasons.add("Cihazda root dosyası bulundu ($path)")
                break
            }
        }

        val pm = context.packageManager
        val rootManagers = listOf("com.topjohnwu.magisk", "eu.chainfire.supersu", "com.noshufou.android.su")
        for (pkg in rootManagers) {
            try {
                getPackageInfo(pm, pkg, 0)
                rootScore += 60
                rootReasons.add("Root yönetim uygulaması yüklü ($pkg)")
            } catch (e: Exception) {
                // Paket yok; beklenen durum.
            }
        }

        if (rootScore >= 50 || rootReasons.size >= 2) {
            threats.add(
                ThreatEntity(
                    packageName = "android.system.root",
                    appName = "Sistem Güvenliği İhlali (Root/Jailbreak)",
                    versionName = "Sistem Çekirdeği",
                    threatCategory = "Cihaz İhlali",
                    riskLevel = if (rootScore >= 80) "KRİTİK" else "YÜKSEK",
                    riskScore = rootScore.coerceIn(50, 100),
                    detectedReasons = rootReasons.joinToString(" • "),
                    status = "AKTİF"
                )
            )
        }
        return threats
    }

    /** Gerçek zamanlı korumanın çalıştığını doğrulamak için zararsız test kaydı üretir. */
    fun createTestThreat(variant: Int = 1): ThreatEntity = when (variant) {
        1 -> ThreatEntity(
            packageName = "com.test.eicar.standard.antivirus",
            appName = "EICAR Test Zararlısı",
            versionName = "2.4.0",
            threatCategory = "Standart Antivirüs Test İmzası",
            riskLevel = "KRİTİK",
            riskScore = 95,
            detectedReasons = "EICAR-Standard-AV-Testfile imzası tespit edildi • Gerçek zamanlı koruma doğrulama testi",
            status = "AKTİF"
        )
        2 -> ThreatEntity(
            packageName = "com.android.fake.bankstealer.sample",
            appName = "Şüpheli Banka Giriş Katmanı",
            versionName = "1.0.1",
            threatCategory = "Ekran Katmanı / Phishing Trojan",
            riskLevel = "KRİTİK",
            riskScore = 90,
            detectedReasons = "SYSTEM_ALERT_WINDOW • Arka planda tuş vuruşlarını izleme • Sahte ödeme arayüzü katmanı",
            status = "AKTİF"
        )
        else -> ThreatEntity(
            packageName = "com.spyware.tracker.sample",
            appName = "Gizli Konum & SMS Casusu",
            versionName = "3.1.2",
            threatCategory = "Casus Yazılım (Spyware)",
            riskLevel = "YÜKSEK",
            riskScore = 80,
            detectedReasons = "Arka planda SMS okuma • Kesintisiz GPS takibi • Bilinmeyen sunucuya veri sızdırma",
            status = "AKTİF"
        )
    }
}
