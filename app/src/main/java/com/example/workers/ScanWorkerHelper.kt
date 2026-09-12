package com.example.workers

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScanWorkerHelper {
    private const val WORK_NAME = "periodic_antivirus_scan"

    fun scheduleScan(context: Context, targetHour: Int, targetMinute: Int) {
        val workManager = WorkManager.getInstance(context)

        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        if (targetTime.before(now)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - now.timeInMillis

        val scanWorkRequest = OneTimeWorkRequestBuilder<PeriodicScanWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("antivirus_scan")
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            scanWorkRequest
        )
    }

    fun cancelScan(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
