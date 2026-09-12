package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ScanRecordEntity
import com.example.data.entity.SecurityEventEntity
import com.example.data.entity.ThreatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AntivirusDao {
    // Threat operations
    @Query("SELECT * FROM threats ORDER BY detectedAt DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats WHERE status = 'AKTİF' ORDER BY riskScore DESC")
    fun getActiveThreats(): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats WHERE status = 'GÜVENLİ_LİSTE' ORDER BY detectedAt DESC")
    fun getWhitelistedThreats(): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats WHERE packageName = :packageName LIMIT 1")
    suspend fun findThreatByPackage(packageName: String): ThreatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreat(threat: ThreatEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreats(threats: List<ThreatEntity>)

    @Update
    suspend fun updateThreat(threat: ThreatEntity)

    @Query("UPDATE threats SET status = :status WHERE id = :id")
    suspend fun updateThreatStatus(id: Long, status: String)

    @Query("DELETE FROM threats WHERE id = :id")
    suspend fun deleteThreatById(id: Long)

    @Query("DELETE FROM threats WHERE packageName = :packageName")
    suspend fun deleteThreatByPackage(packageName: String)

    // Scan record operations
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScanRecords(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestScanRecord(): Flow<ScanRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRecord(record: ScanRecordEntity): Long

    // Security event log operations
    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT 100")
    fun getRecentEvents(): Flow<List<SecurityEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SecurityEventEntity): Long

    @Query("DELETE FROM security_events")
    suspend fun clearEvents()
}
