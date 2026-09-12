package com.example.data.intel

/**
 * Uygulamanın tespit kararlarını besleyen istihbarat kümesi.
 *
 * Tasarım ilkesi: **önce çevrimdışı**. Uygulama internete hiç çıkmasa bile
 * [ThreatIntelStore.baseline] içindeki gömülü küme ile tam işlevsel çalışır.
 * Wi-Fi varken [com.example.worker.MalwareSyncWorker] bu kümeyi tazeler;
 * ağ yoksa, sunucu erişilemezse veya yanıt bozuksa en son geçerli küme kullanılmaya devam eder.
 */
data class ThreatIntel(
    /** İstihbarat sürümü. Sunucudaki sürüm bundan büyük değilse güncelleme uygulanmaz. */
    val revision: Int,

    /** Kümenin nereden geldiği: "bundled" (gömülü) veya feed URL'i. */
    val source: String,

    /** Bu kümenin cihaza yazıldığı an (epoch ms). 0 = hiç senkronize edilmedi. */
    val syncedAt: Long,

    /**
     * Bilinen markaların meşru imzalama sertifikaları: paket adı → izinli SHA-256 özetleri.
     *
     * Bu, klon tespitinin en güçlü hâlidir: `com.whatsapp` paket adını taşıyan bir APK'nın
     * imzası buradaki listede yoksa, paket adı kime ait olursa olsun sahtedir.
     * Gömülü kümede boştur (özetleri uydurmak yanlış pozitif üretir); feed'den doldurulur.
     * Boş olduğunda motor sabitleme (pinning) + kurulum kaynağı sinyallerine düşer.
     */
    val brandCertificates: Map<String, Set<String>>,

    /** Doğrudan zararlı olarak bilinen paket adları (tam eşleşme). */
    val maliciousPackages: Set<String>,

    /** Zararlı yazılım kampanyalarında kullanıldığı bilinen imzalama sertifikası özetleri. */
    val maliciousCertificates: Set<String>,

    /** DNS katmanında engellenecek alan adı anahtar kelimeleri. */
    val blockedDomainKeywords: Set<String>
) {
    val isSynced: Boolean get() = syncedAt > 0L

    /**
     * Paket adı bilinen bir markaya ait mi? (ön ek eşleşmesi)
     *
     * Dikkat: bu **güven** değil, **kimlik iddiası** demektir. Paket adı ucuzdur;
     * herkes `com.whatsapp.pro` adıyla APK paketleyebilir. Motor bunu güvenlik
     * gerekçesi değil, doğrulama yükümlülüğü olarak kullanır.
     */
    fun brandClaimFor(packageName: String): String? =
        KNOWN_BRAND_PREFIXES.entries
            .firstOrNull { (prefix, _) -> packageName == prefix || packageName.startsWith("$prefix.") }
            ?.value

    companion object {
        /**
         * Taklit edilmesi en olası markaların paket ön ekleri → görünen ad.
         *
         * Bu liste bir **beyaz liste değildir.** Eskiden öyleydi: paket adı bu ön eklerden
         * biriyle başlıyorsa tarama hiçbir kontrol yapmadan "temiz" diyordu; yani sahte bir
         * WhatsApp'ın geçmesi garantiliydi. Artık tersi: bu ön ekler ek doğrulama tetikler.
         */
        val KNOWN_BRAND_PREFIXES: Map<String, String> = mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.instagram" to "Instagram",
            "com.facebook" to "Facebook",
            "com.twitter" to "X (Twitter)",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.spotify" to "Spotify",
            "com.netflix" to "Netflix",
            "org.telegram" to "Telegram",
            "com.viber" to "Viber",
            "com.snapchat" to "Snapchat",
            "com.linkedin" to "LinkedIn",
            "com.discord" to "Discord",
            "com.google.android.youtube" to "YouTube",
            "com.microsoft" to "Microsoft",
            "com.skype" to "Skype",
            "com.amazon" to "Amazon",
            "com.paypal" to "PayPal",
            "com.garanti" to "Garanti BBVA",
            "com.akbank" to "Akbank",
            "com.ykb" to "Yapı Kredi",
            "com.isbank" to "İş Bankası",
            "com.ziraat" to "Ziraat Bankası",
            "com.vakifbank" to "VakıfBank",
            "com.finansbank" to "QNB Finansbank",
            "com.denizbank" to "DenizBank",
            "com.teb" to "TEB",
            "com.halkbank" to "Halkbank",
            "tr.gov.turkiye.edevlet.kapisi" to "e-Devlet"
        )

        /**
         * Gömülü temel küme. İnternet olmadan da anlamlı koruma sağlar.
         *
         * Alan adı anahtar kelimeleri bilerek dar tutuldu: "bet", "hack", "tracker" gibi
         * jenerik parçalar etiket sınırına saygı duyan eşleşmede bile meşru alan adlarını
         * vuruyor (`bethesda.com`, `hack.club`, `tracker.debian.org`).
         */
        val BUNDLED = ThreatIntel(
            revision = 1,
            source = "bundled",
            syncedAt = 0L,
            brandCertificates = emptyMap(),
            maliciousPackages = setOf(
                "com.hacker.stealer",
                "com.fake.antivirus",
                "org.malware.dropper",
                "com.crypto.miner.hidden"
            ),
            maliciousCertificates = emptySet(),
            blockedDomainKeywords = setOf(
                "phish", "phishing", "free-robux", "malware", "crypto-miner",
                "cryptominer", "coinhive", "ransomware", "keylogger",
                "stealer-log", "banking-trojan", "fake-update", "apk-crack"
            )
        )
    }
}
