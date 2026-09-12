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

    suspend fun insertThreat(threat: ThreatEntity): Long {
        return dao.insertThreat(threat)
    }

    suspend fun insertThreats(threats: List<ThreatEntity>) {
        dao.insertThreats(threats)
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
