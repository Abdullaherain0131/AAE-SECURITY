package com.example.util

/**
 * DNS sorgularını ayrıştırıp filtreleyen yardımcı.
 *
 * Eskiden filtreleme, ham UDP yükünün ASCII'ye çevrilmiş halinde `contains(keyword)`
 * ile yapılıyordu. Bu yüzden "bet" anahtar kelimesi `bethesda.com`u, "hack"
 * `shackleton.com`u, "tracker" `tracker.debian.org`u engelliyordu. Burada sorgu
 * gerçekten DNS tel formatına göre çözümlenir ve eşleşme alan adı etiketi (label)
 * sınırlarına saygı duyar.
 */
object DnsFilter {

    private const val DNS_HEADER_SIZE = 12
    private const val MAX_LABELS = 64

    /** Anahtar kelime ne kadar isabetli olursa olsun asla engellenmeyecek alan adları. */
    private val ALLOWLIST_SUFFIXES = listOf(
        "google.com", "googleapis.com", "gstatic.com", "googleusercontent.com",
        "android.com", "gvt1.com", "gvt2.com", "ggpht.com", "youtube.com", "ytimg.com",
        "apple.com", "icloud.com", "mzstatic.com",
        "microsoft.com", "windows.net", "live.com", "office.com", "msftconnecttest.com",
        "cloudflare.com", "cloudflare-dns.com", "akamai.net", "akamaiedge.net", "akamaihd.net",
        "amazonaws.com", "cloudfront.net", "fastly.net",
        "whatsapp.net", "whatsapp.com", "facebook.com", "fbcdn.net", "instagram.com",
        "github.com", "githubusercontent.com",
        "turktelekom.com.tr", "turkcell.com.tr", "vodafone.com.tr", "gov.tr", "edu.tr"
    )

    /**
     * Varsayılan engel listesi. Jenerik olduğu için yanlış pozitif üreten kelimeler
     * (`bet`, `hack`, `tracker` gibi) bilerek dışarıda bırakıldı; onlar yerine
     * kendi başına anlamlı olan birleşik ifadeler kullanılıyor.
     */
    val DEFAULT_BLOCKED_KEYWORDS = listOf(
        "phish", "phishing", "free-robux", "malware", "crypto-miner",
        "cryptominer", "coinhive", "ransomware", "keylogger", "stealer-log"
    )

    /**
     * DNS sorgu paketinden ilk sorunun alan adını çıkarır (ör. `ads.example.com`).
     * Sorgu bozuksa, sıkıştırma işaretçisi içeriyorsa veya boşsa null döner.
     */
    fun extractQueryName(dnsPacket: ByteArray, length: Int): String? {
        if (length <= DNS_HEADER_SIZE) return null

        // QR biti 1 ise bu bir yanıt; sorgu bekliyoruz.
        if ((dnsPacket[2].toInt() and 0x80) != 0) return null

        val questionCount = ((dnsPacket[4].toInt() and 0xFF) shl 8) or (dnsPacket[5].toInt() and 0xFF)
        if (questionCount < 1) return null

        val builder = StringBuilder()
        var position = DNS_HEADER_SIZE
        var labelCount = 0

        while (position < length && labelCount++ < MAX_LABELS) {
            val labelLength = dnsPacket[position].toInt() and 0xFF
            if (labelLength == 0) {
                return if (builder.isEmpty()) null else builder.toString()
            }
            // 0xC0: sıkıştırma işaretçisi. Soru bölümünde geçerli değildir.
            if ((labelLength and 0xC0) != 0) return null
            if (position + 1 + labelLength > length) return null

            if (builder.isNotEmpty()) builder.append('.')
            for (i in 0 until labelLength) {
                val byte = dnsPacket[position + 1 + i].toInt() and 0xFF
                builder.append(if (byte in 0x21..0x7E) byte.toChar().lowercaseChar() else '?')
            }
            position += 1 + labelLength
        }
        return null
    }

    /**
     * Alan adı bir anahtar kelimeyle eşleşiyorsa o kelimeyi, eşleşmiyorsa null döner.
     *
     * Eşleşme kuralı: anahtar kelime bir etiketin tamamı olmalı ya da etiket içinde
     * tire ile ayrılmış tam bir parça olmalı. Yani `phish` → `phish.com`,
     * `free-phish.net`, `phish-login.org` eşleşir; `bet` → `bethesda.com` eşleşmez.
     */
    fun findBlockedKeyword(domain: String, keywords: Collection<String>): String? {
        if (domain.isEmpty() || keywords.isEmpty()) return null
        if (isAllowlisted(domain)) return null

        val labels = domain.split('.')
        for (rawKeyword in keywords) {
            val keyword = rawKeyword.trim().lowercase()
            if (keyword.isEmpty()) continue

            // Anahtar kelimenin kendisi nokta içeriyorsa tam alan adı eşleşmesi istenmiştir.
            if (keyword.contains('.')) {
                if (domain == keyword || domain.endsWith(".$keyword")) return keyword
                continue
            }

            if (labels.any { labelContainsToken(it, keyword) }) return keyword
        }
        return null
    }

    fun isAllowlisted(domain: String): Boolean =
        ALLOWLIST_SUFFIXES.any { domain == it || domain.endsWith(".$it") }

    /** Anahtar kelimenin etiket içinde tire sınırlarına oturup oturmadığını kontrol eder. */
    private fun labelContainsToken(label: String, keyword: String): Boolean {
        if (label == keyword) return true
        if (keyword.length > label.length) return false

        var index = label.indexOf(keyword)
        while (index >= 0) {
            val startsAtBoundary = index == 0 || label[index - 1] == '-'
            val endIndex = index + keyword.length
            val endsAtBoundary = endIndex == label.length || label[endIndex] == '-'
            if (startsAtBoundary && endsAtBoundary) return true
            index = label.indexOf(keyword, index + 1)
        }
        return false
    }

    /**
     * Engellenen sorgu için NXDOMAIN yanıtı üretir.
     *
     * Paketi sessizce düşürmek, istemcinin DNS zaman aşımına kadar (çoğu cihazda
     * 5 saniye) beklemesine ve uygulamanın donmuş gibi görünmesine yol açıyordu.
     * NXDOMAIN anında "böyle bir alan adı yok" cevabı verir.
     */
    fun buildNxDomainResponse(query: ByteArray, length: Int): ByteArray? {
        val questionEnd = findQuestionEnd(query, length) ?: return null

        val response = ByteArray(questionEnd)
        System.arraycopy(query, 0, response, 0, questionEnd)

        // Flags baytı 1: QR=1 (yanıt), OPCODE ve RD sorgudan korunur.
        response[2] = (0x80 or (query[2].toInt() and 0x79)).toByte()
        // Flags baytı 2: RA=1 (özyineleme mevcut), RCODE=3 (NXDOMAIN).
        response[3] = 0x83.toByte()

        // QDCOUNT korunur; ANCOUNT / NSCOUNT / ARCOUNT sıfırlanır.
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0
        response[10] = 0; response[11] = 0

        return response
    }

    /** Soru bölümünün bittiği ofseti (QTYPE + QCLASS dahil) döner. */
    private fun findQuestionEnd(packet: ByteArray, length: Int): Int? {
        if (length <= DNS_HEADER_SIZE) return null
        var position = DNS_HEADER_SIZE
        var labelCount = 0

        while (position < length && labelCount++ < MAX_LABELS) {
            val labelLength = packet[position].toInt() and 0xFF
            if (labelLength == 0) {
                val end = position + 1 + 4 // kök etiketi + QTYPE(2) + QCLASS(2)
                return if (end <= length) end else null
            }
            if ((labelLength and 0xC0) != 0) return null
            position += 1 + labelLength
        }
        return null
    }
}
