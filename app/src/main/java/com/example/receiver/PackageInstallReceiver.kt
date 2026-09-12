package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.NewAppInspector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) {
            return
        }

        val uri = intent.data ?: return
        val packageName = uri.schemeSpecificPart ?: return

        // Skip our own package
        if (packageName == context.packageName) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Analiz, kayıt ve bildirim tek merkezde; RealTimeProtectionService'in
                // installer dinleyicisiyle mükerrer uyarı üretmemesi için NewAppInspector kullanılır.
                NewAppInspector.inspectInstalledPackage(
                    context = appContext,
                    packageName = packageName,
                    source = NewAppInspector.SOURCE_BROADCAST
                )
            } catch (e: Exception) {
                android.util.Log.e("PackageInstallReceiver", "Kurulum analizi başarısız: $packageName", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
