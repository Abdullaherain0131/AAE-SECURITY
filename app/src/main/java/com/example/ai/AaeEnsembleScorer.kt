package com.example.ai

/**
 * Özellik vektörü üzerinden risk skorlayan topluluk (ensemble) katmanı.
 *
 * ## Yer tespit hattında nerede duruyor
 *
 * [com.example.scanner.ThreatEngine] dört sinyal katmanından oluşur: kimlik,
 * istihbarat, topluluk skoru ve (güvenilirse) model. Bu sınıf üçüncü katmandır ve
 * [PackageFeatureExtractor] şemasındaki (fs1) 50 özellik üzerinden çalışır — yani
 * TFLite modeliyle **aynı girdiyi** görür. Böylece:
 *
 * * Model `isInferenceTrustworthy` olduğunda (şemaya göre eğitilmiş, `-fsN` sonekli)
 *   iki skor uzlaştırılır; model eğitilmemişken yerine bu skorlayıcı çalışır.
 * * Model yokken "AI skoru" diye uydurma bir sayı göstermek yerine, her puanı
 *   adıyla sınıflandırılabilir bir **katkı listesi** döndürür. Kullanıcı "neden
 *   şüpheli?" sorusuna makul bir cevap alır; karar kutusu değil.
 *
 * ## Tasarım sınırları
 *
 * * Girdi yalnızca [PackageFeatureExtractor.extract] çıktısıdır — PackageManager'e
 *   erişmez, ağa çıkmaz, durum tutmaz. Saf fonksiyondur; aynı vektör her zaman
 *   aynı skoru verir (test edilebilirlik).
 * * Bileşen sayıları (43-46) bazı tarama yollarında sorgulanmamış olabilir ve 0
 *   görünür. "0 etkinlik" ile "sorgulanmadı" ayırt edilemeyeceğinden bu özellikler
 *   yalnızca 0'dan büyükken kanıt sayılır.
 * * Bu bir sezgisel skorlayıcıdır, öğrenilmiş bir model değildir; kendi kimliğini
 *   kullanıcıya "AI" olarak sunmaz. Toplam 0-100 aralığına sıkıştırılır.
 */
object AaeEnsembleScorer {

    /** Bir risk katkısı: insan dilinde etiket + puanı. */
    data class Contribution(val label: String, val points: Int)

    /**
     * @param score 0-100 arası toplam risk skoru.
     * @param contributions skoru oluşturan adlandırılmış katkılar, puanı çoktan aza.
     * @param spyware en az üç ortam izni + internet deseni görüldü mü (kategori seçiminde kullanılır).
     * @param smsAbuse SMS okuma/gönderme + önyükleme + internet deseni görüldü mü.
     * @param overlayAbuse ekran katmanı izni risk sinyali taşıyor mu.
     */
    data class EnsembleVerdict(
        val score: Int,
        val contributions: List<Contribution>,
        val spyware: Boolean,
        val smsAbuse: Boolean,
        val overlayAbuse: Boolean
    )

    // Şema indeksleri — fs1. PackageFeatureExtractor.FEATURE_NAMES ile birebir.
    private const val F_INTERNET = 0
    private const val F_SEND_SMS = 1
    private const val F_RECEIVE_SMS = 2
    private const val F_READ_SMS = 3
    private const val F_RECORD_AUDIO = 6
    private const val F_CAMERA = 7
    private const val F_FINE_LOCATION = 8
    private const val F_BACKGROUND_LOCATION = 9
    private const val F_OVERLAY = 10
    private const val F_INSTALL_PACKAGES = 11
    private const val F_BOOT = 12
    private const val F_ACCESSIBILITY = 13
    private const val F_DEVICE_ADMIN = 14
    private const val F_SRC_SIDELOAD = 26
    private const val F_SRC_UNKNOWN = 27
    private const val F_DEBUG_CERT = 28
    private const val F_SIG_UNREADABLE = 31
    private const val F_BRAND_CLAIM = 32
    private const val F_BRAND_CERT_KNOWN = 33
    private const val F_BRAND_CERT_MISMATCH = 34
    private const val F_INTEL_PACKAGE = 35
    private const val F_INTEL_CERT = 36
    private const val F_DEBUGGABLE = 39
    private const val F_TARGET_SDK = 40
    private const val F_PERMISSION_COUNT = 42
    private const val F_NAME_TOKEN = 47

    fun score(features: FloatArray): EnsembleVerdict {
        val contributions = mutableListOf<Contribution>()
        var spyware = false
        var smsAbuse = false
        var overlayAbuse = false

        fun has(index: Int) = features.getOrNull(index)?.let { it >= 0.5f } == true

        val internet = has(F_INTERNET)
        val hasSms = has(F_SEND_SMS) || has(F_RECEIVE_SMS) || has(F_READ_SMS)
        val spyPermissionCount = listOf(F_RECORD_AUDIO, F_CAMERA, F_FINE_LOCATION, F_BACKGROUND_LOCATION).count(::has)

        // --- Kritik istihbarat eşleşmeleri (doğrudan yüksek puan)
        if (has(F_INTEL_PACKAGE)) contributions += Contribution("Paket adı bilinen zararlı yazılım veritabanında eşleşti", 100)
        if (has(F_INTEL_CERT)) contributions += Contribution("İmza sertifikası bilinen zararlı kampanyalarda kullanılmış", 100)

        // --- Marka taklidi deseni: marka adı taşıyor + imza ispatı yok/uyumsuz
        if (has(F_BRAND_CLAIM) && (has(F_BRAND_CERT_MISMATCH) || !has(F_BRAND_CERT_KNOWN))) {
            contributions += Contribution(
                if (has(F_BRAND_CERT_MISMATCH))
                    "Marka adı taşıyor fakat imza sertifikası uyuşmuyor (klon)"
                else
                    "Marka adı taşıyor fakat imza doğrulaması yapılamadı",
                35
            )
        }

        // --- İzin desenleri (etkileşimli; tek başına izin düşük puan, kombinasyon yüksek)
        if (hasSms && internet && has(F_BOOT)) {
            contributions += Contribution("SMS erişimi + internet + önyükleme: gizlice SMS toplayıp dışarı iletme deseni", 30)
            smsAbuse = true
        } else if (hasSms) {
            contributions += Contribution("SMS okuma/gönderme izni", 12)
        }

        if (has(F_OVERLAY)) {
            contributions += Contribution("Diğer uygulamalar üzerine pencere çizme izni (sahte giriş ekranı vektörü)", 18)
            overlayAbuse = true
        }

        if (spyPermissionCount >= 3 && internet) {
            contributions += Contribution("Kamera/mikrofon/konum izinlerinin internetle birleşmesi (izleme deseni)", 22)
            spyware = true
        } else if (spyPermissionCount == 2) {
            contributions += Contribution("Birden fazla ortam izni (kamera, mikrofon, konum)", 8)
        }

        if (has(F_ACCESSIBILITY) && internet) {
            contributions += Contribution("Erişilebilirlik servisi + internet: ekran okuma ve tuş kaydı potansiyeli", 30)
        } else if (has(F_ACCESSIBILITY)) {
            contributions += Contribution("Erişilebilirlik servisi izni", 14)
        }

        if (has(F_DEVICE_ADMIN) && (has(F_OVERLAY) || has(F_ACCESSIBILITY))) {
            contributions += Contribution("Cihaz yöneticisi + ekran/erişim katmanı: kendini kaldırılamaz yapma deseni", 28)
        }

        if (has(F_INSTALL_PACKAGES) && internet) {
            contributions += Contribution("Başka APK kurma izni + internet: dropper yükleyici deseni", 16)
        }

        // --- Dağıtım kanalı
        if (has(F_SRC_UNKNOWN)) {
            contributions += Contribution("Tanımlanamayan yükleyici tarafından kurulmuş (dropper vektörü)", 18)
        } else if (has(F_SRC_SIDELOAD)) {
            contributions += Contribution("Mağaza dışı (elle kurulmuş) APK", 6)
        }

        // --- İmza kalitesi
        if (has(F_SIG_UNREADABLE)) contributions += Contribution("İmzalama sertifikası okunamıyor (bozuk/kurcalanmış APK)", 20)
        if (has(F_DEBUG_CERT)) contributions += Contribution("Hata ayıklama sertifikasıyla imzalanmış yayın paketi", 10)

        // --- Yapı anomallileri
        if (has(F_DEBUGGABLE)) contributions += Contribution("APK hata ayıklanabilir olarak işaretlenmiş", 10)
        if (has(F_NAME_TOKEN)) contributions += Contribution("Paket/uygulama adı zararlı yazılım terimleri içeriyor", 15)

        // Çok eski hedef SDK: mağaza denetiminden kaçan sideload paketlerinde yaygın.
        val targetSdk = features.getOrNull(F_TARGET_SDK) ?: 0f
        if (targetSdk in 0.01f..(23f / 36f)) {
            contributions += Contribution("Çok eski hedef SDK (Android 6 öncesi davranışıyla çalışıyor)", 8)
        }

        // İzin yoğunluğu: tek tek masum izinlerin birikimi de bir sinyaldir.
        val permissionDensity = features.getOrNull(F_PERMISSION_COUNT) ?: 0f
        if (permissionDensity >= 40f / 80f) {
            contributions += Contribution("Sıra dışı sayıda izin isteniyor (40+)", 6)
        }

        val total = contributions.sumOf { it.points }.coerceAtMost(100)
        return EnsembleVerdict(
            score = total,
            contributions = contributions.sortedByDescending { it.points },
            spyware = spyware,
            smsAbuse = smsAbuse,
            overlayAbuse = overlayAbuse
        )
    }
}
