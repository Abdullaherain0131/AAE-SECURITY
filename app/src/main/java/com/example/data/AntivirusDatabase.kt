package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AntivirusDao
import com.example.data.entity.ScanRecordEntity
import com.example.data.entity.SecurityEventEntity
import com.example.data.entity.ThreatEntity

@Database(
    entities = [
        ThreatEntity::class,
        ScanRecordEntity::class,
        SecurityEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AntivirusDatabase : RoomDatabase() {
    abstract fun antivirusDao(): AntivirusDao

    companion object {
        @Volatile
        private var INSTANCE: AntivirusDatabase? = null

        fun getDatabase(context: Context): AntivirusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AntivirusDatabase::class.java,
                    "antivirus_security_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
