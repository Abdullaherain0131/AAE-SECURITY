package com.example.data.intel

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * [ThreatIntel] kümesini cihazda saklayan tek kaynak.
 *
 * Çevrimdışı garanti: depo boşsa ya da kayıt bozuksa [ThreatIntel.BUNDLED] döner,
 * yani uygulama internete hiç çıkmamış olsa bile motorun çalışacak verisi vardır.
 * Ağdan gelen her güncelleme, uygulanmadan önce [parse] içinde doğrulanır; bozuk
 * bir yanıt mevcut kümeyi bozamaz.
 */
object ThreatIntelStore {

    private const val PREFS = "antivirus_threat_intel"
    private const val KEY_PAYLOAD = "intel_payload"
    private const val TAG = "ThreatIntelStore"

    /** Bir feed yanıtının kabul edilebileceği üst sınırlar (kötü niyetli/bozuk yanıt koruması). */
    private const val MAX_ENTRIES = 5_000
    private const val MAX_TOKEN_LENGTH = 256

    @Volatile
    private var cached: ThreatIntel? = null

    fun current(context: Context): ThreatIntel {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = read(context) ?: ThreatIntel.BUNDLED
            cached = loaded
            return loaded
        }
    }

    /**
     * Yeni kümeyi kalıcı hâle getirir.
     * @return yazıldıysa true; sürüm eski olduğu için atlandıysa false.
     */
    fun save(context: Context, intel: ThreatIntel, force: Boolean = false): Boolean {
        synchronized(this) {
            val existing = current(context)
            if (!force && intel.revision <= existing.revision && existing.isSynced) {
                Log.d(TAG, "Güncelleme atlandı: gelen rev=${intel.revision}, mevcut rev=${existing.revision}")
                return false
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PAYLOAD, serialize(intel).toString())
                .apply()
            cached = intel
            return true
        }
    }

    /** Depoyu temizler; sonraki okuma gömülü kümeye döner. */
    fun reset(context: Context) {
        synchronized(this) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_PAYLOAD).apply()
            cached = null
        }
    }

    private fun read(context: Context): ThreatIntel? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PAYLOAD, null) ?: return null
        return try {
            deserialize(JSONObject(raw))
        } catch (e: Exception) {
            Log.w(TAG, "Kayıtlı istihbarat okunamadı, gömülü kümeye dönülüyor", e)
            null
        }
    }

    // ------------------------------------------------------------------ JSON

    /**
     * Sunucu yanıtını ayrıştırır.
     *
     * Beklenen şema:
     * ```json
     * {
     *   "revision": 42,
     *   "brandCertificates": { "com.whatsapp": ["A1B2...", "C3D4..."] },
     *   "maliciousPackages": ["com.bad.actor"],
     *   "maliciousCertificates": ["EE FF..."],
     *   "blockedDomainKeywords": ["phish", "crypto-miner"]
     * }
     * ```
     * Eksik alanlar gömülü kümedeki karşılıklarıyla doldurulur; bu sayede feed
     * yalnızca değişen bölümü göndererek de güncelleme yapabilir.
     *
     * @return geçerli bir küme, ya da yanıt kullanılamazsa null.
     */
    fun parse(body: String, sourceUrl: String): ThreatIntel? = try {
        val json = JSONObject(body)
        val revision = json.optInt("revision", -1)
        if (revision < 0) {
            Log.w(TAG, "Feed yanıtında geçerli 'revision' yok; güncelleme reddedildi")
            null
        } else {
            ThreatIntel(
                revision = revision,
                source = sourceUrl,
                syncedAt = System.currentTimeMillis(),
                brandCertificates = readDigestMap(json.optJSONObject("brandCertificates")),
                maliciousPackages = readTokenSet(json, "maliciousPackages", ThreatIntel.BUNDLED.maliciousPackages),
                maliciousCertificates = readTokenSet(json, "maliciousCertificates", emptySet()).map { it.uppercase() }.toSet(),
                blockedDomainKeywords = readTokenSet(json, "blockedDomainKeywords", ThreatIntel.BUNDLED.blockedDomainKeywords)
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Feed yanıtı ayrıştırılamadı; mevcut küme korunuyor", e)
        null
    }

    private fun readTokenSet(json: JSONObject, key: String, fallback: Set<String>): Set<String> {
        val array = json.optJSONArray(key) ?: return fallback
        val result = LinkedHashSet<String>()
        for (i in 0 until minOf(array.length(), MAX_ENTRIES)) {
            val token = array.optString(i).trim()
            if (token.isNotEmpty() && token.length <= MAX_TOKEN_LENGTH) result.add(token)
        }
        return result
    }

    private fun readDigestMap(json: JSONObject?): Map<String, Set<String>> {
        if (json == null) return emptyMap()
        val result = LinkedHashMap<String, Set<String>>()
        val keys = json.keys()
        var count = 0
        while (keys.hasNext() && count++ < MAX_ENTRIES) {
            val pkg = keys.next()
            val array = json.optJSONArray(pkg) ?: continue
            val digests = LinkedHashSet<String>()
            for (i in 0 until minOf(array.length(), 32)) {
                // Özetleri normalize et: ayraçsız, büyük harf hex.
                val digest = array.optString(i).replace(":", "").replace(" ", "").uppercase()
                if (digest.length == 64 && digest.all { it in '0'..'9' || it in 'A'..'F' }) digests.add(digest)
            }
            if (digests.isNotEmpty()) result[pkg] = digests
        }
        return result
    }

    private fun serialize(intel: ThreatIntel): JSONObject = JSONObject().apply {
        put("revision", intel.revision)
        put("source", intel.source)
        put("syncedAt", intel.syncedAt)
        put("brandCertificates", JSONObject().apply {
            intel.brandCertificates.forEach { (pkg, digests) ->
                put(pkg, org.json.JSONArray().apply { digests.forEach { put(it) } })
            }
        })
        put("maliciousPackages", org.json.JSONArray().apply { intel.maliciousPackages.forEach { put(it) } })
        put("maliciousCertificates", org.json.JSONArray().apply { intel.maliciousCertificates.forEach { put(it) } })
        put("blockedDomainKeywords", org.json.JSONArray().apply { intel.blockedDomainKeywords.forEach { put(it) } })
    }

    private fun deserialize(json: JSONObject): ThreatIntel = ThreatIntel(
        revision = json.optInt("revision", 0),
        source = json.optString("source", "bundled"),
        syncedAt = json.optLong("syncedAt", 0L),
        brandCertificates = readDigestMap(json.optJSONObject("brandCertificates")),
        maliciousPackages = readTokenSet(json, "maliciousPackages", emptySet()),
        maliciousCertificates = readTokenSet(json, "maliciousCertificates", emptySet()),
        blockedDomainKeywords = readTokenSet(json, "blockedDomainKeywords", ThreatIntel.BUNDLED.blockedDomainKeywords)
    )
}
