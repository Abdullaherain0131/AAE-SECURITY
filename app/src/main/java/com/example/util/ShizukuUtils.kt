package com.example.util

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuUtils {

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return if (isAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        if (isAvailable() && !hasPermission()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    private fun executeCommand(command: String): Boolean {
        if (!isAvailable() || !hasPermission()) return false
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                Log.d("ShizukuUtils", "Output: $line")
            }
            process.waitFor() == 0
        } catch (e: Exception) {
            Log.e("ShizukuUtils", "Failed to execute command: $command", e)
            false
        }
    }

    fun freezeApp(packageName: String): Boolean {
        Log.d("ShizukuUtils", "Freezing app: $packageName")
        return executeCommand("pm disable-user --user 0 $packageName")
    }

    fun unfreezeApp(packageName: String): Boolean {
        Log.d("ShizukuUtils", "Unfreezing app: $packageName")
        return executeCommand("pm enable --user 0 $packageName")
    }

    fun forceStopApp(packageName: String): Boolean {
        Log.d("ShizukuUtils", "Force stopping app: $packageName")
        return executeCommand("am force-stop $packageName")
    }
    
    fun uninstallAppSilent(packageName: String): Boolean {
        Log.d("ShizukuUtils", "Uninstalling app silently: $packageName")
        return executeCommand("pm uninstall --user 0 $packageName")
    }

    fun clearAppData(packageName: String): Boolean {
        Log.d("ShizukuUtils", "Clearing app data: $packageName")
        return executeCommand("pm clear --user 0 $packageName")
    }

    fun grantPermission(packageName: String, permission: String): Boolean {
        Log.d("ShizukuUtils", "Granting permission $permission to $packageName")
        return executeCommand("pm grant $packageName $permission")
    }
}
