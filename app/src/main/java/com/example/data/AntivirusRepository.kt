package com.example.data

import com.example.data.dao.AntivirusDao
import com.example.data.entity.ScanRecordEntity
import com.example.data.entity.SecurityEventEntity
import com.example.data.entity.ThreatEntity
import kotlinx.coroutines.flow.Flow

class AntivirusRepository(private val dao: AntivirusDao) {
    val allThreats: Flow<List<ThreatEntity>> = dao.getAllThreats()
    val activeThreats: Flow<List<ThreatEntity>> = dao.getActiveThreats()
    val whitelistedThreats: Flow<List<ThreatEntity>> = dao.getWhitelistedThreats()
    val scanHistory: Flow<List<ScanRecordEntity>> = dao.getAllScanRecords()
    val latestScan: Flow<ScanRecordEntity?> = dao.getLatestScanRecord()
    val recentEvents: Flow<List<SecurityEventEntity>> = dao.getRecentEvents()

    suspend fun findThreatByPackage(packageName: String): ThreatEntity? {
        return dao.findThreatByPackage(packageName)
    }

    /** Güvenli listeye alınmış paket adları (tarama elemesi için). */
    suspend fun whitelistedPackageNames(): List<String> = dao.getWhitelistedPackageNames()

    suspend fun insertThreat(threat: ThreatEntity): Long {
        return dao.insertThreat(threat)
    }

    suspend fun insertThreats(threats: List<ThreatEntity>) {
        dao.insertThreats(threats)
    }

    /**
     * Bir tespiti kalıcı hâle getirir — tüm tarama yollarının tek giriş noktası.
     *
     * Düz [insertThreat] kullanıldığında iki sorun vardı:
     *
     * 1. **Whitelist dirilmesi.** Tabloda birincil anahtar otomatik id olduğu için
     *    yeniden tespit edilen paket her seferinde *yeni satır* açıyordu; kullanıcı
     *    GÜVENLİ_LİSTE verdiği uygulamanın her tam taramada yeniden AKTİF tehdit
     *    olarak dönmesini izliyordu.
     * 2. **Satır çoğalması.** Aynı paketin tehdit listesi her taramayla büyüyordu.
     *
     * Artık paket bazında tek satır vardır: mevcut kayıt güncellenir, yoksa açılır.
     *
     * @return kayıt yazıldıysa satır id'si; kullanıcı güvenli listeye aldığı için
     *         atlandıysa null.
     */
    suspend fun recordDetectedThreat(threat: ThreatEntity): Long? {
        val existing = dao.findThreatByPackage(threat.packageName)

        // Kullanıcının açık kararı tarama sonucundan üstündür.
        if (existing != null && existing.status == "GÜVENLİ_LİSTE") return null

        if (existing == null) {
            return dao.insertThreat(threat)
        }

        // Eski sürümün satır çoğaltan insert'i biriktirdiği mükerrer kayıtları
        // temizle: bu paket için korunacak tek satır budur.
        dao.pruneDuplicateThreats(threat.packageName, existing.id)

        // Aynı satırı güncelle: kimlik ve ilk tespit zamanı korunur, tespit
        // gerekçeleri ve risk en son analize göre yenilenir.
        dao.updateThreat(
            threat.copy(
                id = existing.id,
                detectedAt = existing.detectedAt,
                status = "AKTİF"
            )
        )
        return existing.id
    }

    suspend fun updateThreatStatus(id: Long, status: String) {
        dao.updateThreatStatus(id, status)
    }

    suspend fun deleteThreatById(id: Long) {
        dao.deleteThreatById(id)
    }

    suspend fun deleteThreatByPackage(packageName: String) {
        dao.deleteThreatByPackage(packageName)
    }

    suspend fun recordScan(record: ScanRecordEntity): Long {
        return dao.insertScanRecord(record)
    }

    suspend fun logEvent(title: String, description: String, severity: String = "INFO"): Long {
        return dao.insertEvent(
            SecurityEventEntity(
                title = title,
                description = description,
                severity = severity
            )
        )
    }

    suspend fun clearEvents() {
        dao.clearEvents()
    }
}
