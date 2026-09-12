package com.example.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import com.example.data.entity.ThreatEntity
import java.io.File

/**
 * Depoda bekleyen, henüz kurulmamış APK dosyalarını tarar.
 *
 * ## Bu sınıf neden var
 *
 * Tam taramanın "Sıkıştırılmış Arşiv", "İndirilen Medya" gibi bölümleri eskiden
 * yalnızca ilerleme çubuğunu oynatan `delay()` çağrılarıydı — hiçbir dosya okunmuyordu.
 * Oysa cihazda iki ciddi vektör bekliyor duruyordu:
 *
 * * **İndirilmiş APK'lar** (İndirilenler klasörü, dosya yöneticisi önbelleği):
 *   kurulmadan önce kimlik/izin analizi yapılamıyordu.
 * * **Aynı APK'nın kopyaları**: zararlı bir APK 5 farklı klasörde yatabilir.
 *
 * `getPackageArchiveInfo` kurulmamış bir APK'nın manifestini (izinler, bileşenler,
 * sertifikalar) PackageManager'a ayrışttırır; [ThreatEngine.evaluatePackageInfo]
 * da bu PackageInfo'yu yüklü bir paketinkinden ayırt etmeden analiz eder. Yani
 * kurulmamış zararlı, kurulu zararlıyla aynı dört katmanlı denetimden geçer.
 *
 * ## Sınırlar
 *
 * * Yalnızca birkaç bilinen klasör taranır (İndirilenler, DCIM, belgeler); tüm
 *   depoyu yürümek dakikalar sürer ve depo taraması antivirüs işi değildir.
 * * Dosya taraması yalnızca izin verilmişse çalışır; `MANAGE_EXTERNAL_STORAGE`
 *   izni yoksa [File.isCanRead] sıfır döner ve tarama sessizce boş sonuç döner.
 * * Analiz ucuz tutulur: her APK için [ThreatEngine] bir kez çalışır, dosya
 *   özetlemeden önce boyut/eklenti elemeden geçirilir.
 */
object ApkFileScanner {

    /** Her klasörde en fazla bu sayıda APK analiz edilir (derin özyineleme koruması). */
    private const val MAX_FILES_PER_DIR = 200

    /** Tarama kapsamına giren depo klasörleri. */
    private val SCAN_DIRS = listOf(
        Environment.DIRECTORY_DOWNLOADS,
        Environment.DIRECTORY_DOCUMENTS,
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_PICTURES,
        Environment.DIRECTORY_MOVIES,
        Environment.DIRECTORY_MUSIC
    )

    /** Bu boyutu aşan APK dosyaları oyun verisi (obb benzeri) olabilir; atlanır. */
    private const val MAX_APK_BYTES = 400L * 1024 * 1024

    data class Result(
        /** İncelenen APK dosya yolları (en fazla raporlama için tutulan sayıda). */
        val scannedFiles: List<String>,
        val threats: List<ThreatEntity>
    )

    /**
     * Bilinen depo klasörlerindeki APK'ları bulur ve analiz eder.
     *
     * @param onProgress ilerleme geri çağrısı: (dosya adı, sıra). UI'da mevcut
     *        adım etiketini güncellemek için.
     */
    fun scanDownloadedApks(
        context: Context,
        onProgress: ((file: File, index: Int) -> Unit)? = null
    ): Result {
        val threats = mutableListOf<ThreatEntity>()
        val scanned = mutableListOf<String>()
        val pm = context.packageManager

        for (dirName in SCAN_DIRS) {
            val dir = Environment.getExternalStoragePublicDirectory(dirName)
            if (!dir.isDirectory || !dir.canRead()) continue

            val apkFiles = try {
                dir.listFiles { file ->
                    file.isFile && file.canRead() &&
                        file.extension.equals("apk", ignoreCase = true) &&
                        file.length() in 1..MAX_APK_BYTES
                }?.sortedBy { it.name }?.take(MAX_FILES_PER_DIR) ?: continue
            } catch (e: Exception) {
                continue
            }

            for ((index, apk) in apkFiles.withIndex()) {
                onProgress?.invoke(apk, index)
                scanned.add(apk.absolutePath)
                try {
                    // İzinler ve bileşen sayıları da gelsin: analiz yüklü paketle
                    // aynı girdi sözleşmesini bekliyor.
                    val flags = PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS
                    val packageInfo = pm.getPackageArchiveInfo(apk.absolutePath, flags) ?: continue
                    // Arşiv PackageInfo'sında uygulama dizini dosyanın kendisidir;
                    // etiket çözümlemesi için PackageManager'a iletilir.
                    packageInfo.applicationInfo?.let { appInfo ->
                        appInfo.sourceDir = apk.absolutePath
                        appInfo.publicSourceDir = apk.absolutePath
                    }

                    val result = ThreatEngine.evaluatePackageInfo(context, packageInfo)
                    val threat = result.threatEntity
                    if (result.isThreat && threat != null) {
                        // Dosya adı, threat kaydının gerekçesinde görünsün: yüklü
                        // paketle karışmasın.
                        threats.add(
                            threat.copy(
                                detectedReasons = "KURULMAMIŞ APK: ${apk.name} • ${threat.detectedReasons}"
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Tek bir bozuk APK tüm taramayı düşürmesin.
                    continue
                }
            }
        }
        return Result(scannedFiles = scanned, threats = threats)
    }
}
